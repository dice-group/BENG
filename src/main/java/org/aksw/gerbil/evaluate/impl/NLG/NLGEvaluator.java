package org.aksw.gerbil.evaluate.impl.NLG;


import java.io.*;
import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.aksw.gerbil.data.SimpleFileRef;
import org.aksw.gerbil.evaluate.DoubleEvaluationResult;
import org.aksw.gerbil.evaluate.EvaluationResultContainer;
import org.aksw.gerbil.evaluate.Evaluator;
import org.apache.commons.io.IOUtils;

public class NLGEvaluator implements Evaluator<SimpleFileRef> {

    public static void main(String[] args) throws IOException, InterruptedException {
        ReaderThread reader = new ReaderThread();
        Thread readerThread = new Thread(reader);
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(new String[]{"./test.sh"});
        Process p = processBuilder.start();
        reader.setInput(p.getInputStream());


        reader.setProcess(p);
        readerThread.start();

        // Wait for the python process to terminate
        int exitValue = p.waitFor();
        // stop the reader thread
        reader.setTerminate(true);
        // Wait for the reader thread to terminate
        readerThread.join();

        // The script encountered an issue
        if (exitValue != 0) {
            // Try to get the error message of the script
            IOUtils.copy(p.getErrorStream(), System.err);
            throw new IllegalStateException("Python script aborted with an error.");
        }

        String scriptResult = reader.getBuffer().toString();
        System.out.println("Data:" + scriptResult + "\n");


    }

    @Override
    public void evaluate(List<List<SimpleFileRef>> annotatorResults, List<List<SimpleFileRef>> goldStandard,
                         EvaluationResultContainer results, String language) {
        // We assume that both lists have only one element!!!
        // We assume that each sub list has exactly one element!!!

        SimpleFileRef expected = goldStandard.get(0).get(0);
        SimpleFileRef hypothesis = annotatorResults.get(0).get(0);

        File ref = expected.getFileRef(); // gives path to file with the expected translation
        File hypo = hypothesis.getFileRef(); // gives path to file with the uploaded translation
        System.out.println("Language: "+ language);
        // start python script and gather results

            String[] command;
            ReaderThread reader = new ReaderThread();
            Thread readerThread = new Thread(reader);
            if (language.equals("ru")){
                command = new String[]{"python3", "src/main/java/org/aksw/gerbil/python/mt/eval.py",
                        "-R", ref.getAbsolutePath(), "-H",hypo.getAbsolutePath(), "-lng", "ru", "-nr", "1",
                        "-m", "bleu,meteor,chrf++,ter"};
            }else{
                command = new String[]{"python3", "src/main/java/org/aksw/gerbil/python/mt/eval.py",
                        "-R", ref.getAbsolutePath(), "-H",hypo.getAbsolutePath(), "-nr", "1",
                        "-m", "bleu,meteor,chrf++,ter"};
           }
        System.out.println("command: "+ command);

        try {

            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command(command);
            Process p = processBuilder.start();
            reader.setInput(p.getInputStream());
            readerThread.start();

            // Wait for the python process to terminate
            int exitValue = p.waitFor();
            // stop the reader thread
            reader.setTerminate(true);
            // Wait for the reader thread to terminate
            readerThread.join();

            // The script encountered an issue
            if (exitValue != 0) {
                // Try to get the error message of the script
                IOUtils.copy(p.getErrorStream(), System.err);
                throw new IllegalStateException("Python script aborted with an error.");
            }

            String scriptResult = reader.getBuffer().toString();
            System.out.println("Data:" + scriptResult + "\n");

            double bleu, nltk, meteor, chrF, ter;
            JsonObject jsonObject = new JsonParser().parse(scriptResult).getAsJsonObject();
            bleu = jsonObject.get("BLEU").getAsDouble();
            results.addResult(new DoubleEvaluationResult("BLEU", bleu));

            nltk = jsonObject.get("BLEU NLTK").getAsDouble();
            results.addResult(new DoubleEvaluationResult("BLEU NLTK", nltk));

            meteor = jsonObject.get("METEOR").getAsDouble();
            results.addResult(new DoubleEvaluationResult("METEOR", meteor));

            chrF = jsonObject.get("chrF++").getAsDouble();
            results.addResult(new DoubleEvaluationResult("chrF++", chrF));

            ter = jsonObject.get("TER").getAsDouble();
            results.addResult(new DoubleEvaluationResult("TER", ter));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static final class ReaderThread implements Runnable {

        private StringBuilder buffer = new StringBuilder();
        private Reader input;
        private Process process;
        private boolean terminate = false;

        @Override
        public void run() {
            try {
                char cBuffer[] = new char[256];
                int length;
                while(process.isAlive()) {
                    while ((length = input.read(cBuffer)) != -1) {
                        buffer.append(cBuffer, 0, length);
                    }
                    // sleep for a short moment before checking the stream again
                    Thread.sleep(50);
                }
                Thread.sleep(50);
                //process is not alive, but terminate should not be set to true, if not then p.waitFor hangs for some reason.
                //This is just a fallback
                if(!terminate){
                    process.destroyForcibly();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public void setProcess(Process p){
            this.process = p;
        }

        public void setInput(InputStream input) {
            this.input = new BufferedReader(new InputStreamReader(input));
        }

        public void setTerminate(boolean terminate) {
            this.terminate = terminate;
        }

        public StringBuilder getBuffer() {
            return buffer;
        }

    }
}