import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
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
    private ArrayList<String> fileNames;
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

        PeerClient peerClient = new PeerClient(ip, port, folderPath, fileNames);

        return peerClient;
    }

    private static void menu(PeerClient peerClient, RequisitionInterface server) throws ServerNotActiveException, RemoteException {
        Scanner sc = new Scanner(System.in);
        int function;

        DownloadRequisitionReceiver drr = new DownloadRequisitionReceiver(peerClient);

        // Inicia thread para tratar requisições de download paralelamente
        Thread drrThread = new Thread(drr);
        drrThread.start();

        do {
            System.out.println("---------Menu do Peer---------");
            System.out.println("Qual a função que deseja usar?");
            System.out.println("1 - JOIN");
            System.out.println("2 - SEARCH");
            System.out.println("3 - DOWNLOAD");
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
                case 0:
                    drrThread.interrupt();
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
        Scanner scanner = new Scanner(System.in);
        System.out.print("Informe o IP do peer a solicitar download: ");
        String ip = scanner.nextLine().trim();
        System.out.print("Informe a porta do peer a solicitar download: ");
        String port = scanner.nextLine().trim();

        if (peerClient.getRequestedFile() == null || peerClient.getRequestedFile().isEmpty()) {
            System.out.print("Favor informar arquivo: ");
            String requestedFile = scanner.nextLine().trim();
            peerClient.setRequestedFile(requestedFile);
        }

        DownloadHandler drs = new DownloadHandler(peerClient, ip, port, server);
        Thread drsThread = new Thread(drs);
        drsThread.start();

    }

    static class DownloadRequisitionReceiver implements Runnable {
        private final PeerClient client;

        public DownloadRequisitionReceiver(PeerClient client) {
            this.client = client;
        }

        @Override
        public void run() {
            try (ServerSocket serverSocket = new ServerSocket(client.getPort())) {

                do {

                    Socket socket = serverSocket.accept();

                    DownloadSender ds = new DownloadSender(socket, client);

                    // Inicia thread para enviar arquivo paralelamente
                    Thread dsThread = new Thread(ds);
                    dsThread.start();

                } while (!Thread.currentThread().isInterrupted());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    static class DownloadHandler implements Runnable {
        private static final int BUFFER_SIZE = 4096;
        private final PeerClient client;
        private final String ip;
        private final int port;
        private final RequisitionInterface server;

        public DownloadHandler(PeerClient client, String ip, String port, RequisitionInterface server) {
            this.client = client;
            this.ip = ip;
            this.port = Integer.parseInt(port);
            this.server = server;
        }

        @Override
        public void run() {
            try {
                Socket socket = new Socket(ip, port);

                OutputStream outputStream = socket.getOutputStream();
                DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
                dataOutputStream.writeUTF(client.getRequestedFile());

                InputStream inputStream = socket.getInputStream();
                DataInputStream dataInputStream = new DataInputStream(inputStream);
                String downloadResponse = dataInputStream.readUTF();

                if (downloadResponse.equals("FILE_NOT_FOUND")) {
                    outputStream.close();
                    dataOutputStream.close();
                    inputStream.close();
                    dataInputStream.close();
                } else {
                    dataOutputStream.writeUTF("INITIATING_DOWNLOAD");

                    String fileName = client.getRequestedFile();

                    if (client.getFileNames().contains(fileName)) {
                        Scanner sc = new Scanner(System.in);
                        System.out.println("Ha um arquivo com mesmo nome na pasta, deseja substituir? [S/N]");
                        String response = sc.nextLine();
                        if (response.equals("S")) {
                            File file = new File(
                                    client.getFolderPath()
                                            + File.separator
                                            + fileName);
                            file.delete();
                        } else {
                            int cont = 1;
                            String baseName = client.getRequestedFile()
                                    .substring(0, client.getRequestedFile().lastIndexOf('.'));
                            String extension = client.getRequestedFile()
                                    .substring(client.getRequestedFile().lastIndexOf('.') + 1);
                            while (client.getFileNames().contains(baseName + "(" + cont + ")" + extension)) cont++;
                            fileName = baseName + "(" + cont + ")" + extension;
                        }

                    }

                    FileOutputStream fileOutputStream = new FileOutputStream(
                            client.getFolderPath()
                                    + File.separator
                                    + fileName);

                    byte[] buffer = new byte[downloadBufferSize(socket.getReceiveBufferSize())];
                    int bytesRead;
                    while ((bytesRead = dataInputStream.read(buffer)) != -1) {
                        fileOutputStream.write(buffer, 0, bytesRead);
                    }

                    System.out.println("Arquivo "
                            + client.getRequestedFile()
                            + " baixado com sucesso na pasta "
                            + client.getFolderPath());

                    client.updateFiles();

                    server.update(client.getIp(), client.getPort(), client.requestedFile);

                    dataOutputStream.close();
                    dataInputStream.close();
                    fileOutputStream.close();
                    inputStream.close();
                    outputStream.close();
                    socket.close();
                }



            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private int downloadBufferSize(int socketBufferSize) {
            int bufferSize = socketBufferSize;
            if (bufferSize < BUFFER_SIZE) {
                bufferSize = BUFFER_SIZE;
            }
            return bufferSize;
        }
    }

    static class DownloadSender implements Runnable {
        private static final int BUFFER_SIZE = 4096;
        private final Socket socket;
        private final PeerClient client;

        public DownloadSender(Socket socket, PeerClient client) {
            this.socket = socket;
            this.client = client;
        }

        @Override
        public void run() {
            try {

                InputStream inputStream = socket.getInputStream();
                DataInputStream dataInputStream = new DataInputStream(inputStream);
                String fileName = dataInputStream.readUTF();

                OutputStream outputStream = socket.getOutputStream();
                DataOutputStream dataOutputStream = new DataOutputStream(outputStream);

                if (!client.getFileNames().contains(fileName)) {

                    dataOutputStream.writeUTF("FILE_NOT_FOUND");

                    dataInputStream.close();
                    inputStream.close();
                    dataOutputStream.close();
                    outputStream.close();
                } else {

                    dataOutputStream.writeUTF("FILE_FOUND");

                    String confirmation = dataInputStream.readUTF();

                    File file = new File(client.getFolderPath(), fileName);
                    FileInputStream fileInputStream = new FileInputStream(file);

                    byte[] buffer = new byte[downloadBufferSize(file.length())];
                    int bytesRead;
                    while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                        dataOutputStream.write(buffer, 0, bytesRead);
                    }

                    dataInputStream.close();
                    inputStream.close();
                    fileInputStream.close();
                    outputStream.close();
                    socket.close();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private int downloadBufferSize(long fileSize) {
            int bufferSize = BUFFER_SIZE;
            if (fileSize > BUFFER_SIZE) {
                while (bufferSize < fileSize && bufferSize < 65535) {
                    bufferSize *= 2;
                }
            }
            return bufferSize;
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

    public void updateFiles() {
        this.fileNames = FileListing(this.getFolderPath());
    }

    public String getRequestedFile() {
        return requestedFile;
    }

    public void setRequestedFile(String requestedFile) {
        this.requestedFile = requestedFile;
    }
}
