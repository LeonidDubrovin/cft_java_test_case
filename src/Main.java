import java.io.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;


public class Main {
    enum DataType {
        INTEGER, STRING
    }

    public static void main(String[] args) throws IOException {
        Instant start = Instant.now();

        boolean sortASC = true;
        DataType dataType = null;

        List<String> fileNames = new ArrayList<>();
        boolean dataTypeSelected = false;
        boolean sortTypeSelected = false;

        for (String arg : args) {
            if (arg.startsWith("-")) {
                if (arg.equals("-i") || arg.equals("-s")) {
                    if (dataTypeSelected) {
                        throw new IllegalArgumentException("Type of data already selected");
                    } else {
                        switch (arg) {
                            case ("-i") -> dataType = DataType.INTEGER;
                            case ("-s") -> dataType = DataType.STRING;
                        }
                        dataTypeSelected = true;
                    }
                }
                if (arg.equals("-a") || arg.equals("-d")) {
                    if (sortTypeSelected) {
                        throw new IllegalArgumentException("Type of sort already selected");
                    } else {
                        switch (arg) {
                            case ("-a") -> sortASC = true;
                            case ("-d") -> sortASC = false;
                        }
                        sortTypeSelected = true;
                    }
                }
            } else {
                fileNames.add(arg);
            }
        }

        if (dataType == null || !dataTypeSelected) {
            throw new IOException("You need argument of data type: -i or -s");
        }

        if (fileNames.size() < 2) {
            throw new IOException("You need to specify at least 2 files: out.txt, in.txt, [in.txt]");
        }

        System.out.println("Arguments is OK");

        if (sortASC)
            System.out.println("sort by ASC");
        else
            System.out.println("sort by DESC");

        if (dataType == DataType.INTEGER)
            System.out.println("sort INTEGER");
        if (dataType == DataType.STRING)
            System.out.println("sort STRING");

        File outputFile = new File(fileNames.get(0));
        List<File> inputFiles = new ArrayList<>();

        for (int i = 1; i < fileNames.size(); i++) {
            String fn = fileNames.get(i);
            File inputFile = new File(fn);
            if (!inputFile.isFile()) {
                throw new IOException(String.format("This file does not exist: %s", inputFile.getAbsolutePath()));
            }
            inputFiles.add(inputFile);
        }

        System.out.println("Start sorting");

        System.out.println("Splitting files");
        List<File> tmpFiles = splitAndSortFiles(inputFiles, sortASC, dataType);

        System.out.println("Merging tmp files");
        mergeSortedTmpFiles(tmpFiles, outputFile, sortASC, dataType);

        System.out.println("End sorting");

        Instant end = Instant.now();
        System.out.printf("execution time milliseconds: %d", Duration.between(start, end).toMillis());
    }

    public static List<File> splitAndSortFiles(List<File> listFiles, boolean sortASC, DataType dataType) throws IOException {
        List<File> tmpFiles = new ArrayList<>();

        for (File file : listFiles) {
            List<String> lineList = new ArrayList<>();
            long maxChunkSize = estimateChunkSize();
            String line = "";

            try (BufferedReader fbr = new BufferedReader(new FileReader(file))) {
                while (line != null) {
                    long currentChunkSize = 0;
                    while (currentChunkSize < maxChunkSize && ((line = fbr.readLine()) != null)) {
                        if (dataType == DataType.STRING)
                            if (line.contains(" "))
                                continue;

                        lineList.add(line);
                        currentChunkSize += line.length();
                    }
                    tmpFiles.add(sortAndSaveTmpFile(lineList, sortASC, dataType));
                    lineList.clear();
                }
            } catch (Exception e) {
                if (lineList.size() > 0) {
                    tmpFiles.add(sortAndSaveTmpFile(lineList, sortASC, dataType));
                    tmpFiles.clear();
                }
            }
        }
        return tmpFiles;
    }

    public static long estimateChunkSize() {
        long freeMemory = Runtime.getRuntime().freeMemory();
        return freeMemory / 4;
    }

    public static File sortAndSaveTmpFile(List<String> lineList, boolean sortASC, DataType dataType) throws IOException {
        var sortedList = mergeSort(lineList.toArray(), sortASC, dataType);

        File tmpFile = File.createTempFile("sortChunk", "tmpfile");
        tmpFile.deleteOnExit();

        try (BufferedWriter fbw = new BufferedWriter(new FileWriter(tmpFile))) {
            for (Object r : sortedList) {
                fbw.write(r.toString());
                fbw.newLine();
            }
        }
        return tmpFile;
    }

    public static void mergeSortedTmpFiles(List<File> tmpFiles, File outputFile, boolean sortASC, DataType dataType) throws IOException {
        PriorityQueue<TmpFileBufferedReader> pq = new PriorityQueue<>(
                new Comparator<TmpFileBufferedReader>() {
                    public int compare(TmpFileBufferedReader one, TmpFileBufferedReader two) {
                        if (sortASC)
                            if (dataType == DataType.STRING) {
                                return one.getHead().compareTo(two.getHead());
                            } else {
                                return Integer.valueOf(one.getHead()).compareTo(Integer.valueOf(two.getHead()));
                            }
                        else if (dataType == DataType.STRING) {
                            return two.getHead().compareTo(one.getHead());
                        } else {
                            return Integer.valueOf(two.getHead()).compareTo(Integer.valueOf(one.getHead()));
                        }

                    }
                }
        );

        for (File file : tmpFiles) {
            TmpFileBufferedReader tfbr = new TmpFileBufferedReader(file);
            pq.add(tfbr);
        }

        try (BufferedWriter fbw = new BufferedWriter(new FileWriter(outputFile))) {
            while (pq.size() > 0) {
                TmpFileBufferedReader fb = pq.poll();
                String line = fb.poll();
                fbw.write(line);
                fbw.newLine();
                if (fb.isEmpty()) {
                    fb.bufferedReader.close();
                    fb.file.delete();
                } else {
                    pq.add(fb);
                }
            }
        } finally {
            for (TmpFileBufferedReader tfbr : pq)
                tfbr.close();
        }
    }

    public static Object[] mergeSort(Object[] arr, boolean sortASC, DataType dataType) {
        if (arr.length == 1) {
            return arr;
        }
        int middle = arr.length / 2;
        var leftHalf = Arrays.copyOfRange(Arrays.stream(arr).toArray(), 0, middle);
        var rightHalf = Arrays.copyOfRange(Arrays.stream(arr).toArray(), middle, arr.length);

        return mergeArrays(mergeSort(leftHalf, sortASC, dataType), mergeSort(rightHalf, sortASC, dataType), sortASC, dataType);
    }

    private static Object[] mergeArrays(Object[] arrOne, Object[] arrTwo, boolean sortASC, DataType dataType) {
        int cntOne = 0, cntTwo = 0;
        Object[] mergedArray = new Object[arrOne.length + arrTwo.length];
        boolean needParseAsString;

        for (int i = 0; i < arrOne.length + arrTwo.length; i++) {
            needParseAsString = false;

            if (dataType == DataType.INTEGER) {
                try {
                    if (cntOne < arrOne.length && cntTwo < arrTwo.length) {
                        if (sortASC) {
                            if (Integer.parseInt(arrOne[cntOne].toString()) < Integer.parseInt(arrTwo[cntTwo].toString())) {
                                mergedArray[i] = arrOne[cntOne++];
                            } else {
                                mergedArray[i] = arrTwo[cntTwo++];
                            }
                        } else {
                            if (Integer.parseInt(arrOne[cntOne].toString()) >= Integer.parseInt(arrTwo[cntTwo].toString())) {
                                mergedArray[i] = arrOne[cntOne++];
                            } else {
                                mergedArray[i] = arrTwo[cntTwo++];
                            }
                        }
                    } else {
                        if (cntOne < arrOne.length) {
                            mergedArray[i] = arrOne[cntOne++];
                        } else {
                            mergedArray[i] = arrTwo[cntTwo++];
                        }
                    }
                } catch (IllegalArgumentException e) {
                    needParseAsString = true;
                }
            }

            if (needParseAsString || dataType == DataType.STRING) {
                if (cntOne < arrOne.length && cntTwo < arrTwo.length) {
                    if (sortASC) {
                        if (arrOne[cntOne].toString().compareTo(arrTwo[cntTwo].toString()) <= 0) {
                            mergedArray[i] = arrOne[cntOne++];
                        } else {
                            mergedArray[i] = arrTwo[cntTwo++];
                        }
                    } else {
                        if (arrOne[cntOne].toString().compareTo(arrTwo[cntTwo].toString()) >= 0) {
                            mergedArray[i] = arrOne[cntOne++];
                        } else {
                            mergedArray[i] = arrTwo[cntTwo++];
                        }
                    }
                } else {
                    if (cntOne < arrOne.length) {
                        mergedArray[i] = arrOne[cntOne++];
                    } else {
                        mergedArray[i] = arrTwo[cntTwo++];
                    }
                }
            }
        }
        return mergedArray;
    }
}
