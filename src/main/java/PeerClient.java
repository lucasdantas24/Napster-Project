import java.io.File;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ServerNotActiveException;
import java.util.ArrayList;
import java.util.Scanner;

import static java.lang.Integer.parseInt;

public class PeerClient {
    private final String ip;
    private final int port;
    private final String folderPath;
    private final ArrayList<String> fileNames;
    private String requestedFile;
    public static final String DEFAULT_IP = "127.0.0.1";
    public static final int DEFAULT_PORT = 1099;


    public PeerClient(String ip, int port, String folderPath, ArrayList<String> fileNames) {
        this.ip = ip;
        this.port = port;
        this.folderPath = folderPath;
        this.fileNames = fileNames;
    }

    public static void main(String[] args) throws NotBoundException, RemoteException, ServerNotActiveException {
        PeerClient peerClient = PeerInicialization();

        Registry registry = LocateRegistry.getRegistry(DEFAULT_IP, DEFAULT_PORT);
        RequisitionInterface server = (RequisitionInterface) registry.lookup("server");

        menu(peerClient, server);
    }

    private static ArrayList<String> FileListing(String folderPath) {
        ArrayList<String> filesNames =  new ArrayList<>();
        File folder = new File(folderPath);
        File[] files = folder.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    String fileName = file.getName();
                    filesNames.add(fileName);
                }
            }
        }
        return filesNames;
    }

    private static PeerClient PeerInicialization() {
        Scanner sc = new Scanner(System.in);
        System.out.print("Favor inserir o IP do cliente:");
        String ip = sc.nextLine().trim();

        System.out.print("Favor inserir a porta do cliente:");
        String portString = sc.nextLine().trim();
        int port = parseInt(portString);

        System.out.print("Favor inserir a pasta do cliente:");
        String folderPath = sc.nextLine().trim();

        ArrayList<String> fileNames = FileListing(folderPath);

        return new PeerClient(ip, port, folderPath, fileNames);
    }

    private static void menu(PeerClient peerClient, RequisitionInterface server) throws ServerNotActiveException, RemoteException {
        Scanner sc = new Scanner(System.in);
        int function;

        do {
            System.out.println("---------Menu do Peer---------");
            System.out.println("Qual a função que deseja usar?");
            System.out.println("1 - JOIN");
            System.out.println("2 - SEARCH");
            System.out.println("3 - DOWNLOAD");
            System.out.println("4 - LOG OUT");
            System.out.println("Aperte 0 para desligar o peer");
            function = sc.nextInt();

            switch (function) {
                case 1:
                    joinReq(peerClient, server);
                    break;
                case 2:
                    searchReq(peerClient, server);
                    break;
                case 3:
                    downloadReq(peerClient, server);
                    break;
                case 4:
                    logOutReq(peerClient, server);
                    break;
            }

        } while (function != 0);
    }

    private static void joinReq(PeerClient peerClient, RequisitionInterface server) throws ServerNotActiveException, RemoteException {
        String response = server.join(peerClient.getIp(), peerClient.getPort(), peerClient.getFileNames());
        if (response.equals("JOIN_OK")) {
            System.out.println("Sou peer "
                    + peerClient.getIp()
                    + ":"
                    + peerClient.getPort()
                    + " com arquivos "
                    + peerClient.getFileNames().toString());
        } else {
            System.out.println("Requisição falhou: " + response);
        }
    }

    private static void searchReq(PeerClient peerClient, RequisitionInterface server) throws ServerNotActiveException, RemoteException {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Informe o arquivo que deseja procurar: ");
        String fileName = scanner.nextLine().trim();

        ArrayList<String> response = server.search(peerClient.getIp(), peerClient.getPort(), fileName);

        System.out.println("peers com arquivo solicitado: " + response.toString());
        if (!response.isEmpty()) peerClient.setRequestedFile(fileName);
    }

    private static void downloadReq(PeerClient peerClient, RequisitionInterface server) {
    }

    private static void logOutReq(PeerClient peerClient, RequisitionInterface server) {
        String response = "as";
    }

    static class downloadRequisitionsHandler implements Runnable {

        @Override
        public void run() {

        }
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public String getFolderPath() {
        return folderPath;
    }

    public ArrayList<String> getFileNames() {
        return fileNames;
    }

    public void setRequestedFile(String requestedFile) {
        this.requestedFile = requestedFile;
    }
}
