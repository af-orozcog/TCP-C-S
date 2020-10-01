package seguridad;

import uniandes.gload.core.LoadGenerator;
import uniandes.gload.core.Task;


public class generador {

	private LoadGenerator generator;

	public generador()
	{
		Task work = createTask();
		int numberofTasks = 200;
		int gapBetweenTasks = 40;
		generator = new LoadGenerator("Client - Server Load Test", numberofTasks, work, gapBetweenTasks);
		generator.generate();

	}

	private Task createTask() {
		return new Main();
	}

	public static void main(String[] args) {
		@SuppressWarnings("unused")
		generador gen = new generador();

	}
}
