package ServerTCP;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.security.MessageDigest;

public class ProtocolThread extends Thread {

	public final static String ARCHIVO_PATH_1 = "data/videos/video1.mp4";
	public final static String ARCHIVO_PATH_2 = "data/videos/video2.mp4";

	public final static int MESSAGE_SIZE = 1024; // Tamaño de los paquetes enviados.

	public String archivo = "";

	private Socket socket = null;

	private boolean conexion;

	private BufferedWriter writer;

	private int numCliente;

	public ProtocolThread(Socket pSocket, int arch, BufferedWriter writer, int numCliente) {
		socket = pSocket;
		this.writer = writer;
		this.numCliente = numCliente;
		if (arch == 1) {
			archivo = ARCHIVO_PATH_1;
		} else
			archivo = ARCHIVO_PATH_2;
		try {
			socket.setSoTimeout(30000);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			try {
				writer.write("Hubo un error con el envío");
				writer.newLine();
				writer.flush();
			} catch (Exception ex) {
				// TODO: handle exception
			}
		}

	}

	public void run() {

		try {
			conexion = true;
			pruebaProcesar();
			conexion = !socket.isClosed();
			socket.close();
		} catch (Exception e) {
			e.printStackTrace();
			try {
				writer.write("Hubo un error con el envío");
				writer.newLine();
				writer.flush();
			} catch (Exception ex) {
				// TODO: handle exception
			}
		}

	}

	public void pruebaProcesar() {

		FileInputStream fis = null;
		BufferedInputStream bis = null;

		OutputStream os = null;

		try {
			os = new BufferedOutputStream(socket.getOutputStream());
			File myFile = new File(archivo);
			byte[] myByteArray = new byte[(int) myFile.length()];
			fis = new FileInputStream(myFile);
			bis = new BufferedInputStream(fis);

			byte[] hash = new byte[61440];
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			hash = md.digest(myByteArray);
			String hashEnviar = new String(hash);
			DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());
			dOut.writeUTF(hashEnviar);

			bis.read(myByteArray, 0, myByteArray.length);

			System.out.println("Enviando (" + myByteArray.length + " bytes)");

			int bytesEnviados = 0;
			int numPaquetes = 0;
			while (bytesEnviados < myByteArray.length) {
				numPaquetes++;
				if ((bytesEnviados + MESSAGE_SIZE) < myByteArray.length) {
					os.write(myByteArray, bytesEnviados, MESSAGE_SIZE);
					bytesEnviados += MESSAGE_SIZE;
				} else {
					int faltaPorEnviar = myByteArray.length - bytesEnviados;
					os.write(myByteArray, bytesEnviados, faltaPorEnviar);
					bytesEnviados += faltaPorEnviar + 1;
				}
			}
			writer.write("Se enviaron en total " + numPaquetes + " paquetes al cliente " + numCliente + " para un toltal de " + numPaquetes*256 + " bytes");
			writer.newLine();
			writer.flush();

			writer.write("Cada paquete con un tamaño de " + MESSAGE_SIZE + " bytes");
			writer.newLine();
			writer.flush();

			os.flush();
			System.out.println("Archivo enviado.");
			writer.write("Archivo enviado exitosamente al cliente " + numCliente);
			writer.newLine();
			writer.flush();

			writer.close();
		} catch (Exception e) {
			System.out.println(e.getMessage());
			conexion = false;
			try {
				writer.write("Hubo un error con el envío");
				writer.newLine();
				writer.flush();
			} catch (Exception ex) {
				// TODO: handle exception
			}
		}

	}
}