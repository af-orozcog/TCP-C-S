package seguridad;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanServer;
import javax.management.ObjectName;

public class MonitorServidor  extends Thread{
	
	
	
	BufferedWriter logW; 
	
	public MonitorServidor(String prueba) throws IOException
	{
		logW = new BufferedWriter(new FileWriter("Prueba" + prueba +".csv",false));
	}
	
	
	public void run() {
		
		try {
			while(true) {
				logW.newLine();
				logW.write(getSystemCpuLoad()+";"+System.nanoTime());
				logW.flush();
				Thread.sleep(67);
			}
			
		} catch (Exception e) {
			System.out.println("Ha ocurrido un error mientras se escribia en el log");
			e.printStackTrace();
		}
		
	}
	public static double getSystemCpuLoad() throws Exception {
		 MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		 ObjectName name = ObjectName.getInstance("java.lang:type=OperatingSystem");
		 AttributeList list = mbs.getAttributes(name, new String[]{ "SystemCpuLoad" });
		 if (list.isEmpty()) return Double.NaN;
		 Attribute att = (Attribute)list.get(0);
		 Double value = (Double)att.getValue();
		 // usually takes a couple of seconds before we get real values
		 if (value == -1.0) return Double.NaN;
		 // returns a percentage value with 1 decimal point precision
		 return ((int)(value * 1000) / 10.0);
		 }

}
