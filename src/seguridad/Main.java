package seguridad;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.security.cert.CertificateException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import uniandes.gload.core.Task;

public class Main extends Task {

	public final static int PUERTO=3400;
	public final static String SERVIDOR="localhost";

	@Override
	public void fail() {
		System.out.println(Task.MENSAJE_FAIL);

	}

	@Override
	public void success() {
		System.out.println(Task.OK_MESSAGE);

	}

	@Override
	public void execute() {
		Security.addProvider((Provider)new BouncyCastleProvider());

		System.out.println("CLIENTE: Conectando al servidor:");
		Socket socket=null;
		PrintWriter escritor=null;
		BufferedReader lector=null;

		try {
			System.out.println("CLIENTE: Conectando al servidor "+SERVIDOR+" en el puerto "+PUERTO);
			socket = new Socket(SERVIDOR,PUERTO);
			escritor=new PrintWriter(socket.getOutputStream(),true);
			lector=new BufferedReader(new InputStreamReader(socket.getInputStream()));
			ProtocoloCliente.procesar(lector,escritor);
			escritor.close();
			lector.close();
			socket.close();
		}
		catch(IOException e)
		{
			e.printStackTrace();
			System.exit(-1);
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CertificateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}
}