import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;

public class Requisitions extends UnicastRemoteObject implements RequisitionInterface {
    public HashMap<String,PeerFiles> joinedPeers = new HashMap<>();

    protected Requisitions() throws RemoteException {
    }

    @Override
    public String join(String ipPeer, int portPeer, ArrayList<String> arquivos) throws RemoteException, ServerNotActiveException {
        String peerKey = ipPeer + ":" + portPeer;
        if (joinedPeers.containsKey(peerKey)) {
            return "Erro! Peer já realizou JOIN!";
        }
        PeerFiles inf = new PeerFiles(ipPeer, portPeer, arquivos);
        joinedPeers.put(peerKey, inf);
        System.out.println("Peer "
                + peerKey
                + " adicionado com arquivos "
                + arquivos.toString());
        return "JOIN_OK";
    }

    @Override
    public ArrayList<String> search(String peerAdress, String filename) throws RemoteException, ServerNotActiveException {
        return null;
    }

    @Override
    public String update(String peerAdress, String filename) throws RemoteException {
        return null;
    }

    @Override
    public String logOut(String ipPeer, int portPeer, ArrayList<String> arquivos) throws RemoteException, ServerNotActiveException {
        return null;
    }

    public static class PeerFiles implements Serializable {
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

}
