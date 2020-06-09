import java.net.Socket;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Scanner;
import java.nio.*;

public class ChatClient {
	
	public static Scanner scanner = new Scanner(System.in);

	public static void main(String[] args) throws Exception {

		Socket socket = new Socket("134.122.27.85", 30124);
		InputStream is = socket.getInputStream();
		OutputStream os = socket.getOutputStream();

		System.out.println("Hello. Enter command.");
		System.out.println("(R) Register");
		System.out.println("(S) Sign in");
		System.out.println("(Q) Quit");

		String command;
		boolean run = true;
		ResponseState responseState = null;

		while(run) {
			command = scanner.nextLine().toUpperCase();

			switch(command) {
			case "R":
				responseState = register(is, os);
				run = false;
				break;
			case "S":
				responseState = signIn(is, os);
				run = false;
				break;
			case "Q":
				run = false;
				break;
			default:
				System.out.println("Unknown command");	
			}	
		}

		System.out.println(responseState);
		if(responseState != ResponseState.SUCCSESS) {
			throw new Exception();
		}

		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				while(true) {
					try { 
						String message = receiveMessage(is);
						System.out.println(message);		
					} catch(Exception e) {
						e.printStackTrace();
					}	
				}				
			}
		};
		new Thread(runnable).start();
		

		while(true) {
				String input = scanner.nextLine();
				String spliter = ":";
				String sendTo = input.substring(0, input.indexOf(spliter)).trim();
				String message = input.substring(input.indexOf(spliter) + 1, input.length()).trim();
				os.write(Operation.WRITE_MESSAGE.ordinal());
				sendString(sendTo, os);
				sendMessage(message, os);
				os.flush(); 
		}		
	}

	public static ResponseState register(InputStream is, OutputStream os) throws Exception {
		os.write(Operation.REGISTER.ordinal());
		System.out.print("Name: ");
		String name = scanner.nextLine();
		sendString(name, os);
		System.out.print("Email: ");
		String email = scanner.nextLine();
		sendString(email, os);
		System.out.print("Password: ");
		String password = scanner.nextLine();
		sendString(password, os);
		os.flush();
		int responseCode = is.read();
		return ResponseState.values()[responseCode];
	}

	public static ResponseState signIn(InputStream is, OutputStream os) throws Exception {
		os.write(Operation.SIGN_IN.ordinal());
		System.out.print("Email: ");
		String email = scanner.nextLine();
		sendString(email, os);
		System.out.print("Password: ");
		String password = scanner.nextLine();
		sendString(password, os);
		os.flush();
		int responseCode = is.read();
		return ResponseState.values()[responseCode];
	}

	public static void sendString(String string, OutputStream os) throws Exception {
		byte[] stringBytes = string.getBytes();
		int length = stringBytes.length;
		os.write(length);
		os.write(stringBytes);
	}

	public static void sendMessage(String string, OutputStream os) throws Exception {
		byte[] stringBytes = string.getBytes();
		int length = stringBytes.length;
		byte[] lengthBytes = ByteBuffer.allocate(2).putShort((short) length).array();
		os.write(lengthBytes);
		os.write(stringBytes);
	}

	public static String receiveMessage(InputStream is) throws Exception {
		byte[] lengthBytes = new byte[2];
		is.read(lengthBytes);
		int length = ByteBuffer.wrap(lengthBytes).getShort();
		byte[] arr = new byte[length];
		is.read(arr);
		return new String(arr);
	}
}

class User {
    final String name;
    final String email;
    final String password;

    User(String name, String email, String password) {
            this.name = name;
            this.email = email; 
            this.password = password;
	}
}

enum Operation {
 	REGISTER,
    SIGN_IN,
 	GET_USERS,
 	WRITE_MESSAGE
}

enum ResponseState {
	SUCCSESS,
	DUBLICATE_EMAIL,
	INVALID_USERNAME_PASSWORD
}



