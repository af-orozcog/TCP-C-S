package ServerTCP;


import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.security.DigestInputStream;
import java.security.MessageDigest;

public class ProtocolThread implements Runnable{
	
	//TAM DE LOS PAQUETES
	private static int PACKAGE = 1024;
	
	//RUTAS ARCHIVOS
	private static String TEXTO_1 = "data/textos/texto1.txt";
	private static String TEXTO_2 = "data/textos/texto2.txt";

	private Socket sc = null;
	private int archivo;
	private int idP;
	private long time_start, time_end, time;

	private static File fileLog;

	/*
	 * Constructor del protocolo del servidor
	 * @param: csP socket designado
	 * @param: idP Numero de thread que atiende
	 */
	public ProtocolThread (Socket csP, int idP, int numArchivo, File archivoLog) {
		
		fileLog = archivoLog;
		sc = csP;
		this.idP=idP;
		archivo = numArchivo;
		this.run();

	}

	/*
	 * Generacion del archivo log. 
	 */
	private void writeLog(String pCadena) {
		synchronized(fileLog)
		{
			try {
				FileWriter fw = new FileWriter(fileLog,true);
				fw.append(pCadena + "\n");
				fw.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}
	
	/**
	 * Método que genera hash para verificar inttegridad
	 * @param ruta ruta del archivo
	 * @param md MessageDigest que iene el algoritmo
	 * @return String hexadecimal con hash de archivo
	 * @throws IOException si falla lectura de archivo
	 */
	private String checksum(String ruta, MessageDigest md ) throws IOException{
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
	
	@Override
	public void run() {

		try {
			
			//RECUPERA EL ARCHIVO A ENVIAR
			File file;
			if(archivo == 1) {
				file = new File(TEXTO_1);
			}
			else {
				file = new File(TEXTO_2);
			}
			
			//CREA BUFFER Y CANALES DE COMUNICACION EN SOCKET
			byte[] archivoBytes = new byte[(int) file.length()];
			FileInputStream fis = new FileInputStream(file);
			BufferedInputStream bis = new BufferedInputStream(fis);
			
			bis.read(archivoBytes, 0, archivoBytes.length);
			OutputStream os = sc.getOutputStream();
			
			//GENERA HASH DEL ARCHIVO PARA COMPROBACION
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			String hexa = checksum(file.getPath(), md);
			
			// ENVÍA EL HASH
			DataOutputStream dos = new DataOutputStream(sc.getOutputStream());
			dos.writeUTF(hexa);
			dos.writeInt((int)file.length());
			
			//NOTIFICA ENVIO DE ARCHIVO Y COMIENZA PROCESO
			System.out.println("Enviando el texto: "+ file.getName() + " de tamanio: " + archivoBytes.length + " Bytes");
			
			int enviados = 0;
			time_start = System.currentTimeMillis();
			for(int i = 0; i <= file.length() - (file.length() % PACKAGE) ; i+=PACKAGE ) {
				if(i == file.length() - (file.length() % PACKAGE)) {
					os.write(archivoBytes, i, (int)file.length() % PACKAGE);
				}
				else {
					os.write(archivoBytes, i, PACKAGE);
				}
				enviados++;
				System.out.println("Paquetes enviados: " + enviados + "/" + (int)(file.length() / PACKAGE));
			}
			
			
			//NOTIFICA TERMINACION DE ENVIO Y RECEPCION DEL ARCHIVO POR PARTE EL CLIENTE
			DataInputStream dis = new DataInputStream(sc.getInputStream());
			if(dis.readByte() == 1) {
				time_end = System.currentTimeMillis();
				time = time_end - time_start;
				String cadena = "Entrega de archivo a cliente " + idP + " fue exitosa. Tomó " + time / 1000 + " segundos";
				writeLog(cadena);
				cadena = "Paquetes enviados " + enviados + " se enviaron " + (int) file.length() + " Bytes";
				writeLog(cadena);
				cadena = "Paquetes recibidos " + enviados + " se recibieron " + (int) file.length() + " Bytes";
				writeLog(cadena);
				System.out.println("Envío de archivo terminado. Cliente ya lo recibió.");
			}
			dis.close();
			bis.close();
			os.close();
			sc.close();
		}
		catch(Exception e) {
			System.out.println("Error en proceso de envío... " + e.getMessage());
		}
		
	}

}