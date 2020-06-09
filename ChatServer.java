import java.io.*;
import java.nio.*;
import java.net.*;
import java.util.*;
public class ChatServer {
        private static Map<String, User> users = new HashMap<>();
        private static Map<String, OutputStream> onlineUsers = new HashMap<>();
        public static void main(String[] args) throws Exception {
                int port = Integer.parseInt(args[0]);
                ServerSocket ss = new ServerSocket(port);
                System.out.println("Server started on port: " + port);
                while(true) {
                        Socket s = ss.accept();
                        System.out.println("Client connected: " + s);
                        Runnable runnable = new Runnable() {
                                @Override
                                public void run() {
                                        try {
                                                process(s);
                                        }catch(Exception e) {
                                                e.printStackTrace();
                                        }
                                }
                        };
                        Thread thread = new Thread(runnable);
                        thread.start();
                }
        }
        private static void process(Socket s) throws Exception {
                int code;
                OpCode opCode;
                InputStream is = s.getInputStream();
                OutputStream os = s.getOutputStream();
                code = is.read();
                opCode = OpCode.values()[code];
                System.out.println("Op code: " + opCode);
                String email;
                switch(opCode) {
                        case Register:
                                email = registerUser(is, os);
                                break;
                        case SignIn:
                                email = signIn(is, os);
                                break;
                        default:
                                throw new IllegalArgumentException();
                }
                if(email == null) {
                        s.close();
                        return;
                }
                while(true) {
                        code = is.read(); // 3
                        System.out.println(">>>>" + code);
                        opCode = OpCode.values()[code];
                        switch(opCode) {
                                case SendMessage:
                                        String to = readString(is); // byte byte[]
                                        System.out.println("To: " + to);
                                        String message = readMessage(is); //byte[2] byte[]
                                        System.out.println("Message: " + message);
                                        OutputStream stream;
                                        if(to.equals("all")) {
                                                for(Map.Entry<String, OutputStream> onlineUser: onlineUsers.entrySet()) {
                                                        if(onlineUser.getKey().equals(email)) continue;
                                                        stream = onlineUser.getValue();
                                                        try {
                                                                sendMessage(stream, email + ": " + message);
                                                        } catch(Exception e) {
                                                                System.out.println("Unable to send to " + onlineUser.getKey());
                                                        }
                                                }
                                        }else {
                                                stream = onlineUsers.get(to);
                                                if(stream == null) return;
                                                sendMessage(stream, email + ": " + message);
                                        }
                                        break;
                        }
                }
        }
        private static String registerUser(InputStream is, OutputStream os) throws Exception {
                String name = readString(is);
                String email = readString(is);
                String password = readString(is);
                User u = users.get(email);
                if(u != null) {
                        System.out.println("Invalid email");
                        os.write(ResponseStatusCode.DUPLICATE_EMAIL.ordinal());
                        return null;
                }
                User user = new User(name, email, password);
                users.put(user.email, user);
                onlineUsers.put(user.email, os);
                System.out.println("[Name: " + name + ", " + "Email: " + email + ", " + "Password: " + password + "]");
                os.write(ResponseStatusCode.SUCCESS.ordinal());
                return email;
        }
        private static String signIn(InputStream is, OutputStream os) throws Exception {
                String email = readString(is);
                String password = readString(is);
                User user = users.get(email);
                if(user == null || !user.password.equals(password)) {
                        os.write(ResponseStatusCode.INVALID_USERNAME_PASSWORD.ordinal());
                        return null;
                }
                onlineUsers.put(email, os);
                os.write(ResponseStatusCode.SUCCESS.ordinal());
                return email;
        }
        private static String readString(InputStream is) throws Exception {
                int length = is.read();
                if(length == -1) throw new IllegalArgumentException();
                byte[] buffer = new byte[length];
                is.read(buffer);
                return new String(buffer);
        }
        private static String readMessage(InputStream is) throws Exception {
                byte[] lengthBytes = new byte[2];
                is.read(lengthBytes);
                short length = ByteBuffer.wrap(lengthBytes).getShort();
                byte[] buffer = new byte[length];
                is.read(buffer);
                return new String(buffer);
        }
        private static void sendMessage(OutputStream os, String message) throws Exception {
                int length;
                byte[] arr;
                arr = message.getBytes();
                length = arr.length;
                byte[] lengthBytes = ByteBuffer.allocate(2).putShort((short) length).array();
                os.write(lengthBytes);
                os.write(arr);
        }
}
enum OpCode {
        Register,
        SignIn,
        GetUsers,
        SendMessage
}
enum ResponseStatusCode {
        SUCCESS,
        DUPLICATE_EMAIL,
        INVALID_USERNAME_PASSWORD
}
class User {
        String name;
        String email;
        String password;
        User(String name, String email, String password) {
                this.name = name;
                this.email = email;
                this.password = password;
        }
}