# Threadtear
Threadtear is a multifunctional deobfuscation tool for java. Suitable for easier code analysis without worrying too much about obfuscation.
Even the most expensive obfuscators like ZKM or Stringer are included. It also contains older deobfuscation tools from my github account. 
![Screenshot 1](https://i.imgur.com/5ihn7qy.png)
![Screenshot 2](https://i.imgur.com/G52XocP.png)
![Screenshot 3](https://i.imgur.com/akzWEtC.png)
## Executions 

An "execution" is a task that is executed and modifies all loaded class files. 
There are multiple types of executions, varying from bytecode cleanup to string deobfuscation. 
Make sure to have them in the right order. Cleanup executions for example should be executed at last.

## Warning
Use this tool at your own risk. Some executions use implemented ClassLoaders to run code from the jar file, an attacker could tweak the file so that malicious code would be executed.
Affected executions use the class `me.nov.threadtear.asm.vm.VM`. These are mostly used for decrypting string or resource obfuscation.

## Make your own execution
You can easily create your own execution task. Just extend `me.nov.threadtear.execution.Execution`:
```java
public class MyExecution extends Execution {
	public MyExecution() {
		super(ExecutionCategory.CLEANING /* category */, "My execution" /* name */,
				"Executes something" /* description, can use html */);
	}
	@Override
	public boolean execute(ArrayList<Clazz> classes, boolean verbose, boolean ignoreErrors) {
		classes.stream().map(c -> c.node).forEach(c -> {
			//transform the classes here using the tree-API of ASM
		});
	}
}
```

## Libraries needed
commons-io 2.6, darklaf-1.3.3.4, asm-all 8+

## License
Threadtear is licensed under the GNU General Public License 3.0

### Notice
Do not deobfuscate any file that doesn't belong to you.  
Please open an issue or send me an email if a transformer doesn't work properly and attach the log.   
Note that output files are most likely not runnable. If you still want to try to run them use `-noverify` as JVM argument!   
This tool is intended for Java 8 but it will probably run on higher versions too. 
<em>*Note that not everything written in this README is implemented yet.*</em>
