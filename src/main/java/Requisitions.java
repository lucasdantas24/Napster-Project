import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Requisitions extends UnicastRemoteObject implements RequisitionInterface {
    public HashMap<String, ArrayList<String>> joinedPeers = new HashMap<>();

    protected Requisitions() throws RemoteException {
    }

    @Override
    public String join(String ipPeer, int portPeer, ArrayList<String> files) throws RemoteException {
        String peerKey = ipPeer + ":" + portPeer;
        if (joinedPeers.containsKey(peerKey)) {
            return "Erro! Peer já realizou JOIN!";
        }
        joinedPeers.put(peerKey, files);
        System.out.println("Peer "
                + peerKey
                + " adicionado com arquivos "
                + files.toString());
        return "JOIN_OK";
    }

    @Override
    public ArrayList<String> search(String ipPeer, int portPeer, String fileName) throws RemoteException {
        String peerKey = ipPeer + ":" + portPeer;
        System.out.println("Peer " + peerKey + " solicitou arquivo " + fileName);
        ArrayList<String> peerWithRequestedFile = new ArrayList<>();
        for (Map.Entry<String, ArrayList<String>> entry : joinedPeers.entrySet()) {
            String key = entry.getKey();
            ArrayList<String> value = entry.getValue();
            if (value.contains(fileName)) peerWithRequestedFile.add(key);
        }
        return peerWithRequestedFile;
    }

    @Override
    public String update(String ipPeer, int portPeer, String fileName) throws RemoteException {
        String peerKey = ipPeer + ":" + portPeer;
        if (joinedPeers.containsKey(peerKey)) {
            ArrayList<String> files = joinedPeers.get(peerKey);
            if (!files.contains(fileName)) {
                files.add(fileName);
                joinedPeers.put(peerKey, files);
            } else {
                return "Arquivo já está registrado";
            }
        } else {
            return "Peer não encontrado";
        }

        return "UPDATE_OK";
    }

}
