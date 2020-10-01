package SinSeguridad;

import uniandes.gload.core.LoadGenerator;
import uniandes.gload.core.Task;


public class Generador {
	private LoadGenerator generator;

	public Generador()
	{
		Task work = createTask();
		int numberofTasks = 400;
		int gapBetweenTasks = 20;
		generator = new LoadGenerator("Client - Server Load Test", numberofTasks, work, gapBetweenTasks);
		generator.generate();

	}

	private Task createTask() {
		return new Main();
	}

	public static void main(String[] args) {
		@SuppressWarnings("unused")
		Generador gen = new Generador();

	}
}
