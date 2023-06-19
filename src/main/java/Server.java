import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

import static java.lang.Integer.parseInt;

public class Server {
    public static final String DEFAULT_IP = "127.0.0.1";
    public static final int DEFAULT_PORT = 1099;

    public static void main(String[] args) {
        try {
            serverInicialization();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void serverInicialization() throws AlreadyBoundException, RemoteException {
        Scanner sc = new Scanner(System.in);
        System.out.print("Favor inserir o IP do registro:");
        String registryIP = sc.nextLine().trim();
        if (registryIP.isEmpty()) registryIP = DEFAULT_IP;

        System.out.print("Favor inserir a porta do registro:");
        String registryPort = sc.nextLine().trim();
        int registryPortNumber;
        if (registryPort.isEmpty()) {
            registryPortNumber = DEFAULT_PORT;
        } else {
            registryPortNumber = parseInt(registryPort);
        }

        serverRMI(registryIP, registryPortNumber);
    }

    private static void serverRMI(String registryIP, int registryPort) throws RemoteException, AlreadyBoundException {
        Requisitions requisitions = new Requisitions();

        LocateRegistry.createRegistry(registryPort);
        Registry reg = LocateRegistry.getRegistry();
        reg.rebind("server", requisitions);
    }


}
