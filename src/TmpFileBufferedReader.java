import java.io.*;

public class TmpFileBufferedReader {
    public BufferedReader bufferedReader;
    public File file;
    private boolean empty;
    private String head;

    public TmpFileBufferedReader(File f) throws IOException {
        file = f;
        bufferedReader = new BufferedReader(new FileReader(file));
        update();
    }

    private void update() throws IOException {
        try {
            empty = (head = bufferedReader.readLine()) == null;
        } catch (EOFException e) {
            head = null;
            empty = true;
        }
    }

    public boolean isEmpty() {
        return empty;
    }

    public String getHead() {
        if (isEmpty())
            return null;
        return head;
    }

    public String poll() throws IOException {
        String resp = getHead();
        update();
        return resp;
    }

    public void close() throws IOException {
        bufferedReader.close();
    }
}
