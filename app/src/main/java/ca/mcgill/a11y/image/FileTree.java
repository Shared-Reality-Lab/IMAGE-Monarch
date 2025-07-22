package ca.mcgill.a11y.image;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import timber.log.Timber;

public class FileTree extends Timber.Tree {

    private static final SimpleDateFormat LOG_LINE_TIME_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    private static final String LOG_FILE_NAME = "logs.txt";
    private static final String[] LOG_LEVELS = {
            "VERBOSE", "DEBUG", "INFO", "WARN", "ERROR", "ASSERT"
    };

    private final PublishSubject<LogElement> logBuffer = PublishSubject.create();
    private static final BehaviorSubject<Long> flush = BehaviorSubject.create();

    private int processedCount = 0;
    private final String filePath;
    private Disposable logSubscription;
    private static final long LOG_FILE_RETENTION = TimeUnit.DAYS.toMillis(7); // for example
    private static final SimpleDateFormat LOG_FILE_TIME_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US);
    private static final long MAX_LOG_FILE_SIZE = 1024 * 1024;

    public FileTree(String filePath) {
        this.filePath = filePath;

        logSubscription = logBuffer
                .observeOn(Schedulers.computation())
                .doAfterNext(logElement -> {
                    processedCount++;
                    if (processedCount % 20 == 0) {
                        flush.onNext(1L);
                    }
                })
                .buffer(Observable.merge(
                        flush,
                        Observable.interval(5, TimeUnit.MINUTES)
                ))
                .subscribeOn(Schedulers.io())
                /*.subscribe(logElements -> {
                    try {
                        File f = getFile(filePath, LOG_FILE_NAME);
                        FileWriter fw = new FileWriter(f, true);
                        for (LogElement el : logElements) {
                            fw.append(el.date).append("\t")
                                    .append(LOG_LEVELS[el.priority]).append("\t")
                                    .append(el.message != null ? el.message : "").append("\n");
                        }
                        fw.flush();
                        fw.close();
                        // Rotate logs right after writing a new batch
                        rotateLogs(filePath, LOG_FILE_NAME);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });*/
                .subscribe(logElements -> {
                    try {
                        File f = getFile(filePath, LOG_FILE_NAME);

                        // ✅ Rotate if log file exceeds max size BEFORE writing
                        if (f.exists() && f.length() >= MAX_LOG_FILE_SIZE) {
                            rotateLogs(filePath, LOG_FILE_NAME);
                        }

                        try (FileWriter fw = new FileWriter(f, true)) {
                            for (LogElement el : logElements) {
                                fw.append(el.date).append("\t")
                                        .append(LOG_LEVELS[el.priority]).append("\t")
                                        .append(el.message != null ? el.message : "").append("\n");
                            }
                            fw.flush();
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
    }

    @Override
    protected void log(int priority, String tag, String message, Throwable t) {
        logBuffer.onNext(new LogElement(
                LOG_LINE_TIME_FORMAT.format(new Date()),
                priority,
                message
        ));
    }

    public static class LogElement {
        public final String date;
        public final int priority;
        public final String message;

        public LogElement(String date, int priority, String message) {
            this.date = date;
            this.priority = priority;
            this.message = message;
        }
    }

    // Helper method to get or create the log file
    private File getFile(String dirPath, String fileName) {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(dir, fileName);
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }

    /**
     * Rotates the given log file: compresses it, truncates it, and deletes old compressed files.
     */
    /*private void rotateLogs(String path, String name) throws IOException {
        File file = getFile(path, name);

        if (!compress(file)) {
            throw new IOException("Failed to compress file for rotation: " + file.getAbsolutePath());
        }

        // Truncate the file to zero bytes
        new PrintWriter(file).close();

        long currentTime = System.currentTimeMillis();

        File[] files = file.getParentFile().listFiles();
        if (files != null) {
            for (File f : files) {
                String ext = getFileExtension(f.getName()).toLowerCase(Locale.ROOT);
                if (ext.equals("gz") && f.lastModified() + LOG_FILE_RETENTION < currentTime) {
                    f.delete();
                }
            }
        }
    }*/

    private void rotateLogs(String path, String name) {
        File logFile = getFile(path, name);
        if (!logFile.exists() || logFile.length() == 0) {
            return;
        }

        // Create rotated file name with timestamp
        String rotatedName = name.substring(0, name.lastIndexOf('.')) +
                "_" + LOG_FILE_TIME_FORMAT.format(new Date()) + ".txt";
        File rotatedFile = new File(logFile.getParentFile(), rotatedName);

        boolean renamed = logFile.renameTo(rotatedFile);
        if (!renamed) {
            System.err.println("Failed to rotate log file");
            return;
        }

        try {
            logFile.createNewFile(); // New empty log file
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Clean up old rotated files
        long currentTime = System.currentTimeMillis();
        File[] files = logFile.getParentFile().listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.getName().startsWith(name.substring(0, name.lastIndexOf('.')) + "_") &&
                        f.getName().endsWith(".txt") &&
                        f.lastModified() + LOG_FILE_RETENTION < currentTime) {
                    f.delete();
                }
            }
        }
    }


    /**
     * Compresses the specified file into GZIP format with a timestamped filename.
     */
    private boolean compress(File file) {
        try {
            String baseName = file.getName().substring(0, file.getName().lastIndexOf('.'));
            File compressed = new File(file.getParentFile(),
                    baseName + "_" + LOG_FILE_TIME_FORMAT.format(new Date()) + ".gz");

            try (FileInputStream input = new FileInputStream(file);
                 FileOutputStream output = new FileOutputStream(compressed);
                 GZIPOutputStream zipped = new GZIPOutputStream(output)) {

                byte[] buffer = new byte[1024];
                int length;
                while ((length = input.read(buffer)) > 0) {
                    zipped.write(buffer, 0, length);
                }

                zipped.finish();
            }
            return true;

        } catch (IOException e) {
            e.printStackTrace(); // Or log with Timber
            return false;
        }
    }

    /**
     * Gets the file extension from a file name.
     */
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return (lastDot >= 0) ? fileName.substring(lastDot + 1) : "";
    }
}
