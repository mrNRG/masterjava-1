package ru.javaops.masterjava.export;

import lombok.extern.slf4j.Slf4j;
import ru.javaops.masterjava.export.helpers.ChunkFuture;
import ru.javaops.masterjava.export.helpers.FailedIndex;
import ru.javaops.masterjava.export.helpers.Helper;
import ru.javaops.masterjava.persist.DBIProvider;
import ru.javaops.masterjava.persist.dao.UserDao;
import ru.javaops.masterjava.persist.model.BaseEntity;
import ru.javaops.masterjava.persist.model.User;
import ru.javaops.masterjava.persist.model.UserFlag;
import ru.javaops.masterjava.xml.util.StaxStreamProcessor;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * gkislin
 * 14.10.2016
 */
@Slf4j
public class UserExport {

    private static final int NUMBER_THREADS = 4;
    private final ExecutorService executorService = Executors.newFixedThreadPool(NUMBER_THREADS);
    private UserDao dao = DBIProvider.getDao(UserDao.class);

    public List<FailedIndex> process(final InputStream is, int chunkSize) throws XMLStreamException {
        log.info("Start proseccing with chunkSize=" + chunkSize);

        Helper helper = Helper.getInstance();

        return new Callable<List<FailedIndex>>() {
            @Override
            public List<FailedIndex> call() throws XMLStreamException {
                List<ChunkFuture<BaseEntity>> futures = new ArrayList<>();

                int id = dao.getSeqAndSkip(chunkSize);
                List<User> chunk = new ArrayList<>(chunkSize);
                final StaxStreamProcessor processor = new StaxStreamProcessor(is);

                while (processor.doUntil(XMLEvent.START_ELEMENT, "User")) {
                    final String email = processor.getAttribute("email");
                    final UserFlag flag = UserFlag.valueOf(processor.getAttribute("flag"));
                    final String fullName = processor.getReader().getElementText();
                    final User user = new User(id++, fullName, email, flag);
                    chunk.add(user);
                    if (chunk.size() == chunkSize) {
                        futures.add(submit(chunk));
                        chunk = new ArrayList<>(chunkSize);
                        id = dao.getSeqAndSkip(chunkSize);
                    }
                }

                if (!chunk.isEmpty()) {
                    futures.add(submit(chunk));
                }
                return helper.getFailedIndex(futures);
            }

            private ChunkFuture submit(List<User> chunk) {
                ChunkFuture chunkFuture = new ChunkFuture<>(chunk,
                        executorService.submit(() -> dao.insertAndGetAlreadyPresent(chunk))
                );
                log.info("Submit " + chunkFuture.getIndexRange());
                return chunkFuture;
            }
        }.call();
    }
}
