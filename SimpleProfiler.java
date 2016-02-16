package simpleProfiler;
import java.io.*;
import java.lang.reflect.*;

//klasa tworzy nowa klase, ktora opakowuje klase wskazana na wejsciu
//i mierzy czas wywolywania poszczegolnych metod klasy bazowej logujac te informacje do pliku
//nalezy dodac projekt z klasa do opakowania (project->properties->javabuildPath->projects)
public class SimpleProfiler{
	private final String nazwaKlasy;
	private final Class<?> klasaBazowa;
	SimpleProfiler(String nk) throws SecurityException, IOException, ClassNotFoundException{
		klasaBazowa = Class.forName(nk);
		nazwaKlasy = klasaBazowa.getSimpleName();
	}
	//funkcja zapisujaca do pliku
	private void zapisz(String file) throws Exception{
		PrintWriter wyjscie;	
		wyjscie = new PrintWriter(file);	//utworzenie strumienia zapisu
		wyjscie.println(generateWrapper());
		wyjscie.close();		//zamknięcie str.
		System.out.println("Utworzono nowy plik "+nazwaKlasy+"Wrapper.java");
	}
	
	private void saveToFile() throws Exception{
		File dir = new File(".\\src\\"+nazwaKlasy+"\\");
		if (!dir.exists())		//jesli nie istnieje folder z nazwa klasy do opakowania, to go utworz
			dir.mkdir();
		//nazwa i sciezka tworzonego pliku
		final String file = ".\\src\\"+nazwaKlasy+"\\"+nazwaKlasy+"Wrapper.java";
		File f = new File(file);	//f - zmienna reprezentująca plik
		if (f.exists()){
			System.out.println("Plik już istnieje. Czy utworzyć nowy? y/n");
			try {
				char opcja = (char) System.in.read();	//pytanie do użytkownika czy korzystać
				if (opcja == 'y')						//z istniejącego pliku
					zapisz(file);
				else{
					System.out.println("pracuję na poprzednim pliku.");
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		} else	//jesli plik nie istnieje
			zapisz(file);		//wywołanie metody zapisującej do strumienia
	}
	//funkcja tworzaca opakowanie klasy podanej na wejsciu. zwraca StringBuilder z ta klasa
	private StringBuilder generateWrapper() throws Exception{
		StringBuilder output = new StringBuilder();
		//naglowek i pola klasy
		output.append("package "+ nazwaKlasy +";\n"+ "import java.util.logging.*;\n\n"+
				"public class "+nazwaKlasy+"Wrapper extends "+nazwaKlasy+"{\n"+
				"  private final Logger logger;\n"+
				"  private final String nazwaKlasy;\n");
		//wyluskanie konstruktorow
		Constructor<?>[] ctors = klasaBazowa.getDeclaredConstructors();
		for(Constructor<?> ctor:ctors){
			Parameter[] pars = ctor.getParameters();
			StringBuilder ctorPars = new StringBuilder();
			StringBuilder rawCtorPars = new StringBuilder();
			for(int i=0; i<pars.length;i++){
				ctorPars.append(pars[i].toString());	//typy i nazwy parametrow
				rawCtorPars.append(pars[i].getName());	//tylko nazwy parametrow
				if(i+1<pars.length){					//formatowanie parametrow
					ctorPars.append(", ");
					rawCtorPars.append(", ");
				}
			}
			//wypisanie konstruktorow uruchamiajacych konstruktory z klasy bazowej i tworzacych obiekt loggera
			output.append("  "+nazwaKlasy+"Wrapper("+ctorPars+") throws Exception{\n"+
					"    super("+ rawCtorPars + ");\n"+
					"    nazwaKlasy ="+"\""+nazwaKlasy+"\""+";\n    logger = Logger.getLogger(nazwaKlasy);\n"+
					"    FileHandler handler = new FileHandler(nazwaKlasy+\"_TimeLog.txt\");\n"+
					"    handler.setFormatter(new SimpleFormatter());\n"+
					"    logger.addHandler(handler);\n}\n");
		}
		//wyluskanie metod innych niz prywatne i main
		Method[] metody = klasaBazowa.getDeclaredMethods();
		for(Method metoda:metody){
			if(metoda.getName() == "main" || metoda.toString().contains("private")) continue;
			String retType = (metoda.getReturnType().isArray()?	//jesli zwracana tablica, to getComponentType()
					metoda.getReturnType().getComponentType().getName()+"[]" : metoda.getReturnType().getName());
			Parameter[] pars = metoda.getParameters();
			StringBuilder parStr = new StringBuilder();
			StringBuilder parNames = new StringBuilder();
			if(pars.length == 0){
				parStr.append("){");
				parNames.append(");");
			}
			for(int i=0; i<pars.length;i++){
				parStr.append(pars[i].toString());	//typy i nazwy parametrow
				parNames.append(pars[i].getName());	//tylko nazwy parametrow
				if(i+1<pars.length){
					parStr.append(", ");
					parNames.append(", ");
				}
				else{
					parStr.append("){");
					parNames.append(");");
				}
			}
			//wypisanie metod
			output.append("  public " +retType+" "+metoda.getName()+"("+parStr+"\n"+
						"    long startTime = System.nanoTime();\n");
			if(retType.isEmpty())
				output.append("    super."+metoda.getName()+"("+parNames+"\n");
			else
				output.append("    "+retType+" result = super."+metoda.getName()+"("+parNames+"\n");
			output.append("    long time = System.nanoTime() - startTime;\n"+
						"    logger.info(\""+metoda.toString()+"\"+\":\"+time+\" ns.\");\n");
						//"    logger.log(Level.INFO,\""+metoda.toString()+"\"+\": \"+time+\" ns.\");\n");
			if(!retType.isEmpty())
				output.append("    return result;\n");
			output.append("  }\n");
		}
		output.append("}\n");
		return output;
	}
	//jako argument nalezy podac kwalifikowana nazwe klasy do opakowania
	//args[0] SkipList.SkipList
	public static void main(String[] args) throws Exception{
		if(args.length != 1)
			System.out.println("Nie podano nazwy klasy do opakowania");
		else{
			SimpleProfiler sp1 = new SimpleProfiler(args[0]);
			sp1.saveToFile();	
		}
	}
}
