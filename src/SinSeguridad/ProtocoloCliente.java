package SinSeguridad;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.xml.bind.DatatypeConverter;

public class ProtocoloCliente {
	/**
	 * Constante de hola
	 */
	public final static String HOLA="HOLA";
	
	/**
	 * constante ok
	 */
	public final static String OK="OK";
	
	/**
	 * constante del algoritmo de cifrado RSA
	 */
	public final static String RSA="RSA";
	
	/**
	 * constante del algoritmo de cifrado AES
	 */
	public final static String AES="AES";
	
	/**
	 * constante para el mensaje de algoritmos
	 */
	public final static String ALGORITMOS="ALGORITMOS";
	
	/**
	 * cosntante para el mensaje de error
	 */
	public final static String ERROR="ERROR";
	
	/**
	 * algortimo de cifrado de HMAC
	 */
	public final static String HMAC="HMACSHA512";
	
	/**
	 * para obtener el ceritifcado del servidor
	 */
	private static X509Certificate certSer;
	
	/**
	 * llave privada
	 */
	private static SecretKey privateKey;
	
	/**
	 * el estado de la comunicación
	 */
	
	/**
	 * se inicializa el estado de la comunicación en falso
	 */
	
	/**
	 * método que dado un algoritmo cifra un mensaje
	 * Este método lo utilzamos para cifrar de manera simetrica  
	 * @param texto el texto que se quiere cifrar
	 * @param algoritmo el algoritmo con el cual se va a cifrar
	 * @return el mensaje cifrado
	 * @throws NoSuchPaddingException 
	 * @throws NoSuchAlgorithmException 
	 */
	public static String cifrar(String texto, String algoritmo){
		if(texto.length()%4!=0) {
			for(int i=0;i<texto.length()%4;i++)
				texto += "0";
		}
		Cipher cifrador;
		try {
			cifrador = Cipher.getInstance(algoritmo);
			cifrador.init(Cipher.ENCRYPT_MODE,privateKey);
			byte secret[] = cifrador.doFinal(DatatypeConverter.parseBase64Binary(texto));
			return DatatypeConverter.printBase64Binary(secret);
		}catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * método para descifrar de manera simetrica con el algoritmo enviado por parametro
	 * @param texto el texto que se quiere cifrar
	 * @param algoritmo el algoritmo con el cual se va a descrifrar
	 * @return el mensaje descifrado
	 */
	public static String descifrar( String texto, String algoritmo){
		try{
			Cipher cifrador=Cipher.getInstance(algoritmo);
			cifrador.init(Cipher.DECRYPT_MODE,privateKey);
			byte serverAns1[] = cifrador.doFinal(DatatypeConverter.parseBase64Binary(texto));
			return DatatypeConverter.printBase64Binary(serverAns1);
		}catch(Exception e){
			System.out.println("Exception " + e.getMessage());
			return null;
		}
	}

	/**
	 * Método para imprimir los mensajes
	 * @param contenido
	 */
	public static void imprimir(byte contenido[]) {
		int i = 0;
		for(; i < contenido.length - 1;++i) 
			System.out.println(contenido[i] + " ");
		System.out.println(contenido[i] + " ");
	}
	
	/**
	 * método qie procesa la comunicación
	 *
	 * @param pIn the in
	 * @param pOut the out
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws NoSuchPaddingException 
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidKeyException 
	 * @throws CertificateException 
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 */
	public static void procesar(BufferedReader pIn, PrintWriter pOut) throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, CertificateException, IllegalBlockSizeException, BadPaddingException {
		//System.out.println("lilil");
		boolean state = false;
		//se empieza la comunicación con el servidor
		 pOut.println(HOLA);
		 String line=pIn.readLine();
		 String answer ="";
		 System.out.println("cliente recibio-" + line + "-continuando.");
		 
		 if(line.equals(OK)&& !state)
		 {
			 answer = ALGORITMOS+":"+AES+":"+RSA+":"+HMAC;
			 pOut.println(answer);
			 System.out.println("cliente envió-" + answer + "-continuando.");
			 state = !state;
		 }
		 
		 line = pIn.readLine();
		 System.out.println("cliente recibio-" + line + "-continuando.");
		 if(line.equals(OK)&& state)
		 {
			 String certificado=pIn.readLine();
			 if(certificado!=null)
			 {
				 //se lee el certificado del servidor
				 byte[] certificadoServidorBytes = DatatypeConverter.parseBase64Binary(certificado);
				 CertificateFactory creator = CertificateFactory.getInstance("X.509");
				 InputStream in = new ByteArrayInputStream(certificadoServidorBytes);
				 certSer = (X509Certificate) creator.generateCertificate(in);
				 System.out.println("Certificado Servidor: " + certSer);
				 certSer.checkValidity();
				 System.out.println("el certificado es válido");
				 //se crea la llave simetrica para la comunicación
				 privateKey = KeyGenerator.getInstance(AES).generateKey();
				 byte key[] = privateKey.getEncoded();
				 //se encripta con la llave publica del servidor
				 answer = DatatypeConverter.printBase64Binary(key);
				 pOut.println(answer);
				 System.out.println("cliente envió llave simétrica-" + answer + "-continuando.");

				 //se envia el reto
				 answer = "casa";
				 pOut.println(answer);
				 System.out.println("el cliente envió reto-" + answer + "-continuando.");

				 //se lee  la respuesta del reto por parte del servidor
				 line = pIn.readLine();
				 System.out.println("el cliente recibio reto-" + line + "-continuando.");

				 //Si son iguales continuo
				 if(line.equals(answer))
					 answer = OK;
				 else {
					 answer = ERROR;
					 pOut.println(answer);
					 return;
				 }
				 //muestra que la respuesta recibida ha sido correcta
				 pOut.println(answer);

				 //se envia la cedula
				 answer = "l32342";
				 pOut.println(answer);
				 System.out.println("cliente envió cédula-" + answer + "-continuando.");

				 //se envia la clave
				 answer = "conTraSeNaS3GuR4";
				 pOut.println(answer);
				 System.out.println("cliente envió contraseña-" + answer + "-continuando.");
				 //se lee el valor que envia el servidor
				 String valor = pIn.readLine();
				 System.out.println("el cliente recibio valor-" + valor + "-continuando.");
				 line = pIn.readLine();
				 byte serverAns1[] = DatatypeConverter.parseBase64Binary(line);

				 //Se descifra con la llave simetrica y utilizando HMAC
				 MessageDigest messageDigest = MessageDigest.getInstance("SHA-512");
				 byte be [] = messageDigest.digest(valor.getBytes());
				 if(Arrays.equals(serverAns1,be))
				 {
					 System.out.println("las funciones de Hash calculadas corresponden a los valores esperados");
					 answer = OK;
				 }
				 else {
					 answer = ERROR;
					 System.out.println(answer);
				 }
				 //cout << ""
				 pOut.println(answer);
				 System.out.println("la conexión se terminada con "+ answer +".");
			 }
		 }
	}
}
