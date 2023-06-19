import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.ServerNotActiveException;
import java.util.ArrayList;


public interface RequisitionInterface extends Remote {

    String join(String ipPeer, int portPeer, ArrayList<String> arquivos) throws RemoteException, ServerNotActiveException;

    ArrayList<String> search(String peerAdress, String filename) throws RemoteException, ServerNotActiveException;

    String update(String peerAdress, String filename) throws RemoteException;

    String logOut(String ipPeer, int portPeer, ArrayList<String> arquivos) throws RemoteException, ServerNotActiveException;

}
