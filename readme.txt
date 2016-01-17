jar usage:
java -cp autotester.jar[;<your_class_path>] espirit.mpoloczek.Launcher <your_main_class> <your_test_config> [<output_folder>]

values inside brackets are technically optional
you usually have to include the jar of the target application in the classpath after autotester.jar
<your_main_class> is the main method of the application to test
<your_test_config> is the absolute path to the config file for the application test
<output_folder> is the absolute path to the output folder for the graph model files, overrides the setting in the config


Config file lines and their function:

targetGUISimpleClassName=Java Simple Classname of the Window/Object/Root of the GUI you want to test. If none is provided or found, the Autotester will print all currently present for you.
testStartDelayInSeconds=Time in seconds before the Autotester starts inputs. Time for the application to launch, load etc
blackListedAbstractButtons=syntax <name>#<windowTitle>#<description>, entries separated by commata, prevent Autotester from inputting into these buttons
blackListedWindowKeywords=entries separated by commata, partial matches suffice, Autotester will skip and destroy any Windows with matching titles
blackListedComponentsSimpleClassNames=entries separated by commata, any GUI objects of these simple classnames will be ignored for input testing
blackListedComponentsByUtilString=entries separated by commata, partial matches suffice, Autotester will skip inputting into any matches. Util String is the internal representation of GUI objects, you see a lot of them in the output.
sleepTimeMillisTextfieldEntries=time in milliseconds between stringlist entries being put into text input objects
sleepTimeMillisBetweenFakeMouseClicks=time in milliseconds between input elements being pressed while testing, use generous values
modelOutputFolder=can also be specified as runtime argument (which overrides this setting), absolute path for the model output file to be saved to.


"runnable_test_example.zip" contains a ready-to-go setup with app to test (yEd), a configuration file, and the autotester application.
Just run the .bat or .sh script file and it should run a test and dump the resulting model file into the folder it was executed in.
If you wish to keep the log, pipe the terminal/console output into a file.