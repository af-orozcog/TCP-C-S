package ClientTCP;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.security.DigestInputStream;
import java.security.MessageDigest;

public class Client {
	
	public static int PUERTO;
	public static String SERVIDOR;
	public static String DIR_DESCARGA = "data/descargas/";
	public static int TAM_BUFFER = 1024;	
	
	public static void main(String[] args) throws IOException{
		
		//COMIENZA EJECUCION EN MAQUINA CLIENTE
		System.out.println("Cliente...");
		
		//PREPARACION DE SOCKET Y DE CONSOLA PARA ENTRADA DE USUARIO
		Socket socket = null;
		InputStreamReader isr = new InputStreamReader(System.in);
		BufferedReader br = new BufferedReader(isr);
		
		try {
			//PETICION DE DATOS PARA COMENZAR INTENTO DE CONEXION
			System.out.println("Ingresa la dirección IP del servidor");
			SERVIDOR = br.readLine();
			System.out.println("Ingresa el puerto para conectarte");
			PUERTO = Integer.parseInt(br.readLine());
			
			//CREACION DE SOCKET CON SERVIDOR
			socket = new Socket(SERVIDOR,PUERTO);
			if(!socket.isClosed()) {
				
				//CONEXION CON SERVIDOR Y NOTIFICACION DE ESTADO DE CONEXION
				System.out.println("Estado de conexión: " + !socket.isClosed() + " Esperando ID y nombre de archivo...");
			}
			
			//CREACION DE CANAL ENTRADA Y SALIDA EN SOCKET
			DataOutputStream dout = new DataOutputStream(socket.getOutputStream());
			DataInputStream din = new DataInputStream(socket.getInputStream());
			int id;
			String nombreArchivo;
			
			//PROTOCOLO PENSADO: CUANDO SERVIDOR CREA SOCKET, ENVÍA "0", LUEGO EL ID DEL CLIENTE Y LUEGO EL NOMBRE DE ARCHIVO QUE ENVIA
			if(din.readByte() == 0) {
				id = din.readInt();
				nombreArchivo = din.readUTF();
				System.out.println("El archivo a recibir es "+ nombreArchivo + " y su ID es " + id );
				
				//ENVIAR NOTIFICACION DE PREPARADO PARA RECIBIR ARCHIVO
				System.out.println("Notificando al servidor que se está esperando el archivo...");
				dout.writeUTF("OK");
				
				//LLAMADO A METODO PARA RECIBIR ARCHIVO
				descargar(socket, id, nombreArchivo);
				System.out.println("terminé descarga :D");
			}
			else {
				System.out.println("Falla de servidor");
			}
			
		}
		catch(IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		
		br.close();
		socket.close();
	}
	
	/**
	 * Método que recibe archivo del servidor
	 * @param s socket creado con servidor
	 * @param id id del cliente asignado por servidor
	 * @param arch nombre de archivo a descargar
	 */
	public static void descargar(Socket s, int id, String arch) {
		
		try {
			
			//CANAL DE LECTURA DE ARCHIVO 
			FileOutputStream fos = new FileOutputStream(DIR_DESCARGA + arch);
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			InputStream is = s.getInputStream();
			
			//RECEPCION DE HASH DEL SERVIDOR
			
			DataInputStream dis = new DataInputStream(s.getInputStream());
			String hashServidor = dis.readUTF();
			int archivoBytes = dis.readInt();
			byte[] buffer = new byte[archivoBytes];
			
			for(int i = 0; i <= buffer.length - (buffer.length % TAM_BUFFER); i +=TAM_BUFFER ) {
				if(i == buffer.length - (buffer.length % TAM_BUFFER)) {
					is.read(buffer, i, buffer.length % TAM_BUFFER);
				}
				else {
					is.read(buffer, i, TAM_BUFFER);
				}
			}
			//ESCRIBE ARCHIVO
			bos.write(buffer, 0, buffer.length);
					
			bos.flush();
			
			//NOTIFICACION DE FINALIZACION AL USUARIO Y A SERVIDOR
			System.out.println("Archivo " + arch + " descargado en " + DIR_DESCARGA + " (" + buffer.length + " Bytes leídos).");
			
			//VERIFICACION DE INTEGRIDAD DE ARCHIVO
			MessageDigest sha = MessageDigest.getInstance("SHA-1");
			String hashCliente = checksum(DIR_DESCARGA+arch, sha);
			System.out.println("Comenzando a verificar integridad de archivo descargado...");
			
			if(hashCliente.equals(hashServidor)) {
				//CONFIRMA RECEPCION DE ARCHIVO
				DataOutputStream dos = new DataOutputStream(s.getOutputStream());
				dos.writeByte(1);
				
				System.out.println("El archivo recibido ha sido examinado y no ha sido manipulado.");
			}
			else {
				System.out.println("¡¡ADVERTENCIA!! El archivo descargado no es igual al original");
			}
			
			dis.close();
			bos.close();
			fos.close();
			s.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public static String checksum(String ruta, MessageDigest md) throws IOException{
		try (DigestInputStream dis = new DigestInputStream(new FileInputStream(ruta), md)) {
            while (dis.read() != -1) ; //empty loop to clear the data
            md = dis.getMessageDigest();
        }

        // bytes to hex
        StringBuilder result = new StringBuilder();
        for (byte b : md.digest()) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
	}
}