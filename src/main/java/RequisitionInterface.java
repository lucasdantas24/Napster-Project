import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.ServerNotActiveException;
import java.util.ArrayList;


public interface RequisitionInterface extends Remote {

    String join(String ipPeer, int portPeer, ArrayList<String> files) throws RemoteException, ServerNotActiveException;

    ArrayList<String> search(String ipPeer, int portPeer, String fileName) throws RemoteException, ServerNotActiveException;

    String update(String ipPeer, int portPeer, String fileName) throws RemoteException;

}
