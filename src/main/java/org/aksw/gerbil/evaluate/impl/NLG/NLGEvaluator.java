package org.aksw.gerbil.evaluate.impl.NLG;


import java.io.*;
import java.util.List;
import java.util.UUID;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.aksw.gerbil.data.SimpleFileRef;
import org.aksw.gerbil.datatypes.ErrorTypes;
import org.aksw.gerbil.evaluate.DoubleEvaluationResult;
import org.aksw.gerbil.evaluate.EvaluationResultContainer;
import org.aksw.gerbil.evaluate.Evaluator;
import org.aksw.gerbil.exceptions.GerbilException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NLGEvaluator implements Evaluator<SimpleFileRef> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NLGEvaluator.class.getName());

    @Override
    public void evaluate(List<List<SimpleFileRef>> annotatorResults, List<List<SimpleFileRef>> goldStandard,
                         EvaluationResultContainer results, String language) throws GerbilException {
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
        System.out.println("command: "+ String.join(" ", command));

        try {

            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command(command);
            //redirect error to file so it won't block
            String errorFileName = UUID.randomUUID().toString();
            File errorFile = new File(errorFileName);
            errorFile.createNewFile();
            processBuilder.redirectError(ProcessBuilder.Redirect.to(errorFile));
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
                LOGGER.error(FileUtils.readFileToString(errorFile));
                errorFile.delete();

                throw new GerbilException("Python script aborted with an error.", ErrorTypes.UNEXPECTED_EXCEPTION);
            }
            errorFile.delete();
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
        private boolean terminate = false;

        @Override
        public void run() {
            try {
                char cBuffer[] = new char[256];
                int length;
                while (!terminate) {
                    while ((input != null) && (input.ready())) {
                        length = input.read(cBuffer);
                        buffer.append(cBuffer, 0, length);
                    }
                    // sleep for a short moment before checking the stream again
                    Thread.sleep(50);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
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