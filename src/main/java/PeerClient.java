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

    // Construtor da Classe
    public PeerClient(String ip, int port, String folderPath, ArrayList<String> fileNames) {
        this.ip = ip;
        this.port = port;
        this.folderPath = folderPath;
        this.fileNames = fileNames;
    }

    // Main da Classe
    public static void main(String[] args) throws NotBoundException, RemoteException, ServerNotActiveException {
        // Inicia uma instância do Peer para guardar informações
        PeerClient peerClient = PeerInicialization();

        // Conecta ao serviço do servidor via RMI

        // Localiza o registro do servidor usando IP e porta padrão
        Registry registry = LocateRegistry.getRegistry(DEFAULT_IP, DEFAULT_PORT);
        // Conecta ao serviço server instanciado no servidor
        RequisitionInterface requisitions = (RequisitionInterface) registry.lookup("requisitions");

        // Inicia o menu interativo
        menu(peerClient, requisitions);
    }

    // Função para listar arquivos de uma pasta
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

    // Iniciador do peer
    private static PeerClient PeerInicialization() {
        // Captura IP do Cliente
        Scanner sc = new Scanner(System.in);
        System.out.print("Favor inserir o IP do cliente:");
        String ip = sc.nextLine().trim();

        // Captura porta do Cliente
        System.out.print("Favor inserir a porta do cliente:");
        String portString = sc.nextLine().trim();
        int port = parseInt(portString);

        // Captura caminho da pasta em que os arquivos serão manipulados
        System.out.print("Favor inserir a pasta do cliente:");
        String folderPath = sc.nextLine().trim();

        // Gera array com nomes de arquivos que estão dentro da pasta
        ArrayList<String> fileNames = FileListing(folderPath);

        // Cria a instância do Peer com as informações capturadas
        return new PeerClient(ip, port, folderPath, fileNames);
    }

    private static void menu(PeerClient peerClient, RequisitionInterface server) throws ServerNotActiveException, RemoteException {
        Scanner sc = new Scanner(System.in);
        String function;

        // Instanciando Thread que gerenciará os downloads
        DownloadRequisitionReceiver drr = new DownloadRequisitionReceiver(peerClient);

        // Inicia thread para tratar requisições de download paralelamente
        Thread drrThread = new Thread(drr);
        drrThread.start();

        // Loop para menu interativo
        do {
            System.out.println("---------Menu do Peer---------");
            System.out.println("Qual a funcao que deseja usar?");
            System.out.println("JOIN");
            System.out.println("SEARCH");
            System.out.println("DOWNLOAD");
            System.out.println("Aperte 0 para desligar o peer");
            function = sc.nextLine().trim();

            switch (function) {
                case "JOIN":
                    joinReq(peerClient, server);
                    break;
                case "SEARCH":
                    searchReq(peerClient, server);
                    break;
                case "DOWNLOAD":
                    downloadReq(peerClient, server);
                    break;
                case "0":
                    // Interrompe thread que gerencia downloads no caso de o usuário desligar o peer no console
                    drrThread.interrupt();
                    break;
            }

        } while (!function.equals("0"));
    }

    // Função que implementa o JOIN do lado do peer
    private static void joinReq(PeerClient peerClient, RequisitionInterface server) throws ServerNotActiveException, RemoteException {
        // Envia requisição de JOIN via RMI
        String response = server.join(peerClient.getIp(), peerClient.getPort(), peerClient.getFileNames());
        // Se receber JOIN_OK, escreve mensagem padrão de apresentação, caso contrário mostra mensagem de erro
        if (response.equals("JOIN_OK")) {
            System.out.println("Sou peer "
                    + peerClient.getIp()
                    + ":"
                    + peerClient.getPort()
                    + " com arquivos "
                    + String.join(" ", peerClient.getFileNames()));
        } else {
            System.out.println("Requisicao falhou: " + response);
        }
    }

    // Função que implementa o SEARCH do lado do peer
    private static void searchReq(PeerClient peerClient, RequisitionInterface server) throws ServerNotActiveException, RemoteException {
        // Captura arquivo a ser procurado
        Scanner scanner = new Scanner(System.in);
        System.out.print("Informe o arquivo que deseja procurar: ");
        String fileName = scanner.nextLine().trim();

        // Envia requisição SEARCH via RMI e recebe lista de peer com o arquivo
        ArrayList<String> response = server.search(peerClient.getIp(), peerClient.getPort(), fileName);

        // Mostra mensagem na tela com peers que possuem arquivo desejado
        System.out.println("peers com arquivo solicitado: " + String.join(" ", response));

        // Muda arquivo requisitado para o arquivo procurado caso exista algum peer com o arquivo desejado
        if (!response.isEmpty()) peerClient.setRequestedFile(fileName);
    }

    // Função que implementa a requisição de DOWNLOAD
    private static void downloadReq(PeerClient peerClient, RequisitionInterface server) {
        Scanner scanner = new Scanner(System.in);

        // Pede para o usuário informar o arquivo caso não tenha sido setado anteriormente
        if (peerClient.getRequestedFile() == null || peerClient.getRequestedFile().isEmpty()) {
            System.out.print("Favor informar arquivo a ser baixado: ");
            String requestedFile = scanner.nextLine().trim();
            peerClient.setRequestedFile(requestedFile);
        }

        // Capturando IP e porta do peer ao qual será solicitado o DOWNLOAD
        System.out.print("Informe o IP do peer a solicitar download: ");
        String ip = scanner.nextLine().trim();
        System.out.print("Informe a porta do peer a solicitar download: ");
        String port = scanner.nextLine().trim();

        // Instanciando thread que realizará o download
        DownloadHandler drs = new DownloadHandler(peerClient, ip, port, server);

        // Inicia thread que realizará o download
        Thread drsThread = new Thread(drs);
        drsThread.start();
    }


    // Thread que gerencia requisições de DOWNLOAD enviadas por outros peers
    static class DownloadRequisitionReceiver implements Runnable {
        private final PeerClient client;

        public DownloadRequisitionReceiver(PeerClient client) {
            this.client = client;
        }

        @Override
        public void run() {
            // Iniciando socket de servidor
            try (ServerSocket serverSocket = new ServerSocket(client.getPort())) {

                do {
                    // Iniciando Listener para receber TCP
                    Socket socket = serverSocket.accept();

                    // Após receber a comunicação TCP, instancia thread para enviar arquivo paralelamente
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

    // Thread que gerencia o DOWNLOAD
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
                // Iniciando conexão TCP com outro peer
                Socket socket = new Socket(ip, port);

                // Envia nome de arquivo desejado via TCP
                OutputStream outputStream = socket.getOutputStream();
                DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
                dataOutputStream.writeUTF(client.getRequestedFile());

                // Recebe resposta do outro peer
                InputStream inputStream = socket.getInputStream();
                DataInputStream dataInputStream = new DataInputStream(inputStream);
                String downloadResponse = dataInputStream.readUTF();

                // Verificando resposta do outro peer
                if (downloadResponse.equals("FILE_NOT_FOUND")) {
                    // Fecha socket e streams caso o peer informe que não encontrou o arquivo
                    System.out.println("Arquivo não encontrado no peer informado");
                    outputStream.close();
                    dataOutputStream.close();
                    inputStream.close();
                    dataInputStream.close();
                } else {
                    // Manda mensagem para outro peer que está se preparando para o download
                    dataOutputStream.writeUTF("INITIATING_DOWNLOAD");

                    String fileName = client.getRequestedFile();

                    // Verifica se não há arquivo com mesmo nome na pasta
                    if (client.getFileNames().contains(fileName)) {

                        // Altera nome de arquivo para que não ocorra erro
                        int cont = 1;
                        String baseName = client.getRequestedFile()
                                .substring(0, client.getRequestedFile().lastIndexOf('.'));
                        String extension = client.getRequestedFile()
                                .substring(client.getRequestedFile().lastIndexOf('.') + 1);
                        while (client.getFileNames().contains(baseName + "(" + cont + ")." + extension)) cont++;
                        fileName = baseName + "(" + cont + ")" + extension;

                    }

                    // Iniciando stream para escrever arquivo na pasta
                    FileOutputStream fileOutputStream = new FileOutputStream(
                            client.getFolderPath()
                                    + File.separator
                                    + fileName);

                    // Cria buffer com tamanho dinâmico baseado no tamanho do buffer do socket
                    byte[] buffer = new byte[downloadBufferSize(socket.getReceiveBufferSize())];
                    int bytesRead;
                    // Lê bytes enviados via TCP e escreve no arquivo
                    while ((bytesRead = dataInputStream.read(buffer)) != -1) {
                        fileOutputStream.write(buffer, 0, bytesRead);
                        fileOutputStream.flush();
                    }

                    // Mostra mensagem de confirmação do download
                    System.out.println("Arquivo "
                            + client.getRequestedFile()
                            + " baixado com sucesso na pasta "
                            + client.getFolderPath());

                    // Envia requisição UPDATE via RMI
                    String response = server.update(client.getIp(), client.getPort(), fileName);

                    if (response.equals("UPDATE_OK")) {
                        // Atualiza os arquivos do peer
                        client.updateFiles();
                    }

                    // Fechando socket e streams
                    dataOutputStream.close();
                    dataInputStream.close();
                    fileOutputStream.close();
                    inputStream.close();
                    outputStream.close();
                }
                socket.close();


            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Função para calcular tamanho do buffer
        private int downloadBufferSize(int socketBufferSize) {
            int bufferSize = socketBufferSize;
            if (bufferSize < BUFFER_SIZE) {
                bufferSize = BUFFER_SIZE;
            }
            return bufferSize;
        }
    }

    // Thread que gerencia envio de arquivo a um outro peer
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

                // Recebe nome de arquivo requisitado
                InputStream inputStream = socket.getInputStream();
                DataInputStream dataInputStream = new DataInputStream(inputStream);
                String fileName = dataInputStream.readUTF();

                // Inicia o outputStream para enviar informações via TCP
                OutputStream outputStream = socket.getOutputStream();
                DataOutputStream dataOutputStream = new DataOutputStream(outputStream);

                // Verifica se o arquivo requisitado está na pasta do peer
                if (!client.getFileNames().contains(fileName)) {
                    // Caso o arquivo não seja encontrado manda mensagem informando via TCP
                    dataOutputStream.writeUTF("FILE_NOT_FOUND");

                    dataInputStream.close();
                    inputStream.close();
                    dataOutputStream.close();
                    outputStream.close();
                } else {
                    // Manda confirmação de que o arquivo foi encontrado
                    dataOutputStream.writeUTF("FILE_FOUND");

                    String confirmation = dataInputStream.readUTF();

                    // Prepara arquivo para enviar
                    File file = new File(client.getFolderPath(), fileName);
                    FileInputStream fileInputStream = new FileInputStream(file);

                    // Cria buffer com base no tamanho do arquivo
                    byte[] buffer = new byte[downloadBufferSize(file.length())];
                    int bytesRead;
                    // Envia arquivo via TCP
                    while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                        dataOutputStream.write(buffer, 0, bytesRead);
                        dataOutputStream.flush();
                    }

                    // Fecha socket e streams
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

        // Função para calcular tamanho do buffer
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
