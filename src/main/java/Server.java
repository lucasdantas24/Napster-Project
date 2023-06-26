import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
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
        // Captura IP do registro, caso não seja informado usa o valor padrão
        Scanner sc = new Scanner(System.in);
        System.out.print("Favor inserir o IP do registro:");
        String registryIP = sc.nextLine().trim();
        if (registryIP.isEmpty()) registryIP = DEFAULT_IP;

        // Captura porta do registro, caso não seja informado usa o valor padrão
        System.out.print("Favor inserir a porta do registro:");
        String registryPort = sc.nextLine().trim();
        int registryPortNumber;
        if (registryPort.isEmpty()) {
            registryPortNumber = DEFAULT_PORT;
        } else {
            registryPortNumber = parseInt(registryPort);
        }

        // Chama função que inicia o RMI no lado do servidor
        serverRMI(registryPortNumber);
    }

    private static void serverRMI(int registryPort) throws RemoteException, AlreadyBoundException {
        // Instancia classe com serviços que serão usados via RMI
        Requisitions requisitions = new Requisitions();

        // Cria registro de RMI
        LocateRegistry.createRegistry(registryPort);
        Registry reg = LocateRegistry.getRegistry();
        // Faz rebind do serviço com o registro
        reg.rebind("requisitions", requisitions);
    }

    public static class Requisitions extends UnicastRemoteObject implements RequisitionInterface {
        // Estrutura em que as informações sobre os peers são guardadas
        public HashMap<String, ArrayList<String>> joinedPeers = new HashMap<>();

        protected Requisitions() throws RemoteException {
        }

        @Override
        public String join(String ipPeer, int portPeer, ArrayList<String> files) throws RemoteException {
            // Cria a chave do HashMap usando IP e porta
            String peerKey = ipPeer + ":" + portPeer;

            // Adiciona peer e lista de arquivos no HashMap
            joinedPeers.put(peerKey, files);

            // Mostra mensagem com informações do peer adicionado
            System.out.println("Peer "
                    + peerKey
                    + " adicionado com arquivos "
                    + String.join(" ", files));
            return "JOIN_OK";
        }

        @Override
        public ArrayList<String> search(String ipPeer, int portPeer, String fileName) throws RemoteException {
            // Cria a chave do HashMap usando IP e porta
            String peerKey = ipPeer + ":" + portPeer;

            // Mostra mensagem informando peer e arquivo pedido
            System.out.println("Peer " + peerKey + " solicitou arquivo " + fileName);

            // Verifica e envia peers com arquivo solicitado
            ArrayList<String> peersWithRequestedFile = new ArrayList<>();
            for (Map.Entry<String, ArrayList<String>> entry : joinedPeers.entrySet()) {
                String key = entry.getKey();
                ArrayList<String> value = entry.getValue();
                if (value.contains(fileName)) peersWithRequestedFile.add(key);
            }
            return peersWithRequestedFile;
        }

        @Override
        public String update(String ipPeer, int portPeer, String fileName) throws RemoteException {
            // Cria a chave do HashMap usando IP e porta
            String peerKey = ipPeer + ":" + portPeer;

            // Verifica se o peer já realizou o JOIN e se o arquivo não foi registrado e atualiza caso contrário
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


}
