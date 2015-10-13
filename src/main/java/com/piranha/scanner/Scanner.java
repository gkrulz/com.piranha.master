package com.piranha.scanner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.piranha.scanner.model.JavaClass;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Padmaka on 9/8/2015.
 */
public class Scanner {
    private static final Logger log = Logger.getLogger(Scanner.class);
    private ArrayList<JavaClass> javaClasses;

    public Scanner(){
        javaClasses = new ArrayList<JavaClass>();
    }

    public Collection read(){
        File file = new File(System.getProperty("user.dir")+"/src/main/resources/src");
        Collection files = FileUtils.listFiles(file, new RegexFileFilter("^(.*?)"), DirectoryFileFilter.DIRECTORY);
        return files;
    }

    public void findDependencies(ArrayList<File> files) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        for(File f : files){

            log.debug(f.getName());
            InputStreamReader inputStreamReader = new InputStreamReader(new FileInputStream(f));

            String fileString = FileUtils.readFileToString(f, inputStreamReader.getEncoding());

            JsonObject classJson = this.getClass(fileString);

                try {
                    log.debug(classJson.get("classDeclaration").getAsString());
//                    log.debug(matcher.end());
//                    log.debug(fileString.charAt(matcher.end() - 1));
                    this.findClasses(f.getName(), classJson.get("classDeclaration").getAsString(), fileString, classJson.get("end").getAsInt() - 1);
//                    String str = fileString.substring(classJson.get("end").getAsInt(), endOfClass);
//                    JavaClass javaClass = new JavaClass();
//                    javaClass.setDeclaration(classJson.get("classDeclaration").getAsString());
//                    javaClass.setBody(str);
//                    javaClasses.add(javaClass);
//                    log.debug("------------------------------------------------------");
//                    log.debug(str);
//                    log.debug("------------------------------------------------------");
                }catch (IllegalStateException e){
                    System.err.println(f.getName() + " - No match found");
                }


        }
        log.debug(gson.toJson(classes));
    }

    public JsonObject getClass(String fileString){

        Pattern pattern = Pattern.compile("(((public|protected|private|)?(\\s+abstract)?(\\s+static)?\\s+class\\s+(\\w+)((\\s+extends\\s+\\w+)|(\\s+implements\\s+\\w+\\s*(,\\s*\\w+\\s*)*)|(\\s+extends\\s+\\w+\\s+implements\\s+\\w+\\s*(,\\s*\\w+\\s*)*))?\\s*\\{)|" +
                "((public|protected)?(\\s+abstract)?(\\s+static)?\\s+interface\\s+(\\w+)(\\s+extends\\s+\\w+\\s*(,\\s*\\w+\\s*)*)?\\s*\\{))");

        Matcher matcher = pattern.matcher(fileString);
        matcher.find();

        JsonObject classJson = new JsonObject();
        classJson.addProperty("end", matcher.end());
        classJson.addProperty("classDeclaration", matcher.group());
        return classJson;
    }

    private ArrayList<JsonObject> classes = new ArrayList<JsonObject>();

    public void findClasses(String fileName, String className, String fileString, int startOfClass){

        int endOfClass = 0;
        Stack<Character> stack = new Stack<Character>();

        for (int i = startOfClass; i < fileString.length(); i++) {

            char current = fileString.charAt(i);
            if (current == '{') {
                stack.push(current);
            }
            if (current == '}') {
                char last = stack.peek();
                if (current == '}' && last == '{') {
                    stack.pop();
                }
                else{
                    endOfClass = -1;
                }

                if (stack.isEmpty()){
//                    endOfClass = i;
                    String classString = fileString.substring(startOfClass, i);
                    JsonObject classJson = new JsonObject();
                    classJson.addProperty("file", fileName);
                    classJson.addProperty("className", className);
                    classJson.addProperty("classSting", classString);
                    classes.add(classJson);

                    if (i < fileString.length()){
                        String restOfTheString = fileString.substring(i, fileString.length());
                        JsonObject nextClass = this.getClass(restOfTheString);
                        this.findClasses(fileName, nextClass.get("classDeclaration").getAsString(), restOfTheString, nextClass.get("end").getAsInt() - 1);
                    }
                    break;
                }
            }
        }
    }
}