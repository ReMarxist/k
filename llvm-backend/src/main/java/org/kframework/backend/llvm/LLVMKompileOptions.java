// Copyright (c) Runtime Verification, Inc. All Rights Reserved.
package org.kframework.backend.llvm;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.Parameter;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.kframework.utils.inject.RequestScoped;

@RequestScoped
public class LLVMKompileOptions {

  @Inject
  public LLVMKompileOptions() {}

  @Parameter(
      names = "--enable-llvm-debug",
      description = "Enable debugging support for the LLVM backend.")
  public boolean debug = false;

  @Parameter(
      names = "-ccopt",
      description = "Add a command line option to the compiler invocation for the llvm backend.",
      descriptionKey = "options",
      listConverter = SingletonListConverter.class,
      hidden = true)
  public List<String> ccopts = new ArrayList<>();

  public static class SingletonListConverter implements IStringConverter<List<String>> {
    @Override
    public List<String> convert(String str) {
      return Arrays.asList(str);
    }
  }

  @Parameter(
      names = "--heuristic",
      description =
          "A string of single characters representing a sequence of heuristics to use during"
              + " pattern matching compilation. Valid choices are f, d, b, a, l, r, n, p, q, _, N,"
              + " L, R.",
      descriptionKey = "heuristics",
      hidden = true)
  public String heuristic = "qbaL";

  @Parameter(
      names = "--iterated",
      description =
          "Generate iterated pattern matching optimization; time-consuming but significantly"
              + " reduces matching time.",
      hidden = true)
  public boolean iterated = false;

  @Parameter(
      names = "--iterated-threshold",
      description =
          "Threshold heuristic to use when choosing which axioms to optimize. A value of 0 turns"
              + " the optimization off; a value of 1 turns the optimization on for every axiom."
              + " Values in between (expressed as a fraction of two integers, e.g. 1/2), control"
              + " the aggressiveness of the optimization. Higher values increase compilation times"
              + " extremely, but also increase the effectiveness of the optimization. Consider"
              + " decreasing this threshold if compilation is too slow.",
      descriptionKey = "value",
      hidden = true)
  public String iteratedThreshold = "1/2";

  @Parameter(
      names = "--no-llvm-kompile",
      description =
          "Do not invoke llvm-kompile. Useful if you want to do it yourself when building with the"
              + " LLVM backend.",
      hidden = true)
  public boolean noLLVMKompile;

  @Parameter(
      names = "--enable-search",
      description =
          "By default, to reduce compilation time, `krun --search` is disabled on the LLVM backend."
              + " Pass this flag to enable it.")
  public boolean enableSearch;

  @Parameter(
      names = "--llvm-kompile-type",
      description =
          "Specifies the llvm backend's output type. Valid choices are main (build an interpreter),"
              + " library (build an interpreter, but don't link a main function), search (same as"
              + " main, but the interpreter does search instead of single-path execution), static"
              + " (same as library, but no '-l' flags are passed during linking. Used for making a"
              + " partially linked object file) and python (build a Python bindings module for this"
              + " definition)",
      descriptionKey = "type",
      hidden = true)
  public String llvmKompileType = "main";

  @Parameter(
      names = "--llvm-kompile-output",
      description = "Name of the output binary from the llvm backend.",
      descriptionKey = "file",
      hidden = true)
  public String llvmKompileOutput = null;
}
