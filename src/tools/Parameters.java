package tools;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import validator.BottomUpMatrix_DS;
import validator.BottomUpMatrix_SS;
import validator.BottomUpQueue_DS;
import validator.Naive_DS;
import validator.TopDownMatrix_DS;
import validator.Validator;

public class Parameters {
  private static final Options argOptions;

  static {
    Options options = new Options();
    options.addOption(
        Option.builder("n").hasArg(false).desc("Represents an index").build());
    options.addOption("useautomata", false,
        "Use automata to check equivalency");
    options.addOption("skipequiv", false, "Skip equivalency check");
    options.addOption("compact", false, "Use memory-compact version");
    options.addOption("noearlyeval", false,
        "Do not use early evaluation strategies");
    options.addOption("noearlyprune", false,
        "Do not use early pruning strategies");
    options.addOption("nolengthfilter", false,
        "Do not use length filter strategies");
    options.addOption(Option.builder("v").argName("VALIDATOR")
        .desc("BottomUpMatrixDS: \n" + "BottomUpMatrixSS: \n"
            + "BottomUpQueue: \n" + "TopDownMatrixDS: \n" + "Naive: ")
        .numberOfArgs(2).build());
    options
        .addOption(
            Option.builder("joinExpandThreshold").argName("T")
                .desc("If number of expanded record is less of equal to T,"
                    + " use naive method (for hybrid algorithms only)")
        .build());
    argOptions = options;
  }

  public static Parameters parseArgs(String[] args) {
    CommandLineParser parser = new DefaultParser();
    Parameters param = new Parameters();
    try {
      CommandLine cmd = parser.parse(argOptions, args);
      ValidatorName vname = ValidatorName.BottomUpQueueDS;
      int vthreshold = 100;
      if (cmd.hasOption("n"))
        param.maxIndex = Integer.parseInt(cmd.getOptionValue("n"));
      if (cmd.hasOption("useautomata")) {
        param.useACAutomata = true;
        param.compact = false;
      }
      if (cmd.hasOption("skipequiv")) param.skipChecking = true;
      if (cmd.hasOption("compact")) {
        param.compact = true;
        param.useACAutomata = false;
      }
      if (cmd.hasOption("noearlyeval")) param.earlyeval = false;
      if (cmd.hasOption("noearlyprune")) param.earlyprune = false;
      if (cmd.hasOption("nolengthfilter")) param.useLengthFilter = false;
      if (cmd.hasOption("v")) {
        vname = ValidatorName.valueOf(cmd.getOptionValue("v"));
        if (vname == ValidatorName.Naive)
          vthreshold = Integer.parseInt(cmd.getOptionValues("v")[1]);
      }
      if (cmd.hasOption("joinExpandThreshold")) param.joinThreshold = Integer
          .parseInt(cmd.getOptionValue("joinExpandThreshold"));
      // Wrap up
      switch (vname) {
        case BottomUpMatrixDS:
          param.validator = new BottomUpMatrix_DS(param.earlyeval,
              param.earlyprune);
          break;
        case BottomUpMatrixSS:
          param.validator = new BottomUpMatrix_SS(param.earlyeval,
              param.earlyprune);
          break;
        case BottomUpQueueDS:
          param.validator = new BottomUpQueue_DS(param.useLengthFilter);
          break;
        case TopDownMatrixDS:
          param.validator = new TopDownMatrix_DS();
          break;
        case Naive:
          param.validator = new Naive_DS(vthreshold);
          break;
        default:
          throw new Exception("Unknown validator name");
      }
      String[] remainingArgs = cmd.getArgs();
      if (remainingArgs.length != 4)
        throw new Exception("Number of remaining args is not 4");
      param.inputX = remainingArgs[0];
      param.inputY = remainingArgs[1];
      param.inputRules = remainingArgs[2];
      param.output = remainingArgs[3];
    } catch (Exception e) {
      e.printStackTrace(System.out);
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("[OPTIONS] <inputX> <inputY> <rules> <output>",
          argOptions, true);
      System.exit(0);
    }
    return param;
  }

  private enum ValidatorName {
    BottomUpMatrixDS, BottomUpMatrixSS, BottomUpQueueDS, TopDownMatrixDS, Naive
  }

  boolean   useACAutomata   = false;
  boolean   skipChecking    = false;
  int       maxIndex        = 1;
  boolean   compact         = true;
  boolean   singleside      = false;
  boolean   earlyprune      = true;
  boolean   earlyeval       = true;
  boolean   useLengthFilter = true;
  int       joinThreshold   = 100;
  Validator validator;
  String[]  remainingArgs;

  String    inputX;
  String    inputY;
  String    inputRules;
  String    output;

  public boolean isUseACAutomata() {
    return useACAutomata;
  }

  public boolean isSkipChecking() {
    return skipChecking;
  }

  public int getMaxIndex() {
    return maxIndex;
  }

  public boolean isCompact() {
    return compact;
  }

  public boolean isSingleside() {
    return singleside;
  }

  public boolean isEarlyprune() {
    return earlyprune;
  }

  public boolean isEarlyeval() {
    return earlyeval;
  }

  public boolean isUseLengthFilter() {
    return useLengthFilter;
  }

  public int getJoinThreshold() {
    return joinThreshold;
  }

  public Validator getValidator() {
    return validator;
  }

  public String[] getRemainingArgs() {
    return remainingArgs;
  }

  public String getInputX() {
    return inputX;
  }

  public String getInputY() {
    return inputY;
  }

  public String getInputRules() {
    return inputRules;
  }

  public String getOutput() {
    return output;
  }
}
