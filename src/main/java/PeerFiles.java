import java.io.Serializable;
import java.util.ArrayList;

public class PeerFiles implements Serializable {
    private final String ip;
    private final int port;
    private final ArrayList<String> filesNames;

    public PeerFiles(String ip, int port, ArrayList<String> filesNames) {
        this.ip = ip;
        this.port = port;
        this.filesNames = filesNames;
    }

    public void addFile(String fileName) {
        filesNames.add(fileName);
    }

    public void removeFile(String fileName) {
        filesNames.remove(fileName);
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public ArrayList<String> getFilesNames() {
        return filesNames;
    }


}
