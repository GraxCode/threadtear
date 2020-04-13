# Threadtear
Threadtear is a multifunctional deobfuscation tool for java. Suitable for easier code analysis without worrying too much about obfuscation.
Will also support ZKM, Stringer and other obfuscators. It also combines older tools from my github account.
![Screenshot 1](https://i.imgur.com/XXGmbFD.png)
## Executions

An "execution" is a task that is run and modifies all loaded class files. 
There are multiple types of executions, varying from bytecode cleanup to string deobfuscation. 
Make sure to have them in the right order. Cleanup executions for example should be executed at last.

## Libraries needed
commons-io 2.6, darklaf-1.3.3.4, asm-all 8+

## License
Threadtear is licensed under the GNU General Public License 3.0

#### Notice
Do not deobfuscate any file that doesn't belong to you.  
Please open an issue or send me an email if a transformer doesn't work properly and attach the log.   
Note that output files are most likely not runnable. If you still want to try to run them use "-noverify" as JVM argument!   
This tool is intended for Java 8 but it will probably run on higher versions too. 
Note that not everything written in this README is implemented yet.
