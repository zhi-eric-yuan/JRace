# Modified from http://www.cs.ubc.ca/labs/beta/Projects/MIP-Config/run_cplex12.rb
# Modification author: Zhi Eric Yuan

require 'tmpdir'
require 'fileutils'

cplex_executable_absolute_filename = "~/Applications/IBM/ILOG/CPLEX_Studio126/cplex/bin/x86-64_osx/cplex"
tmpdir = "."

def output_help(out)
	out.puts "This starts an interactive session of CPLEX 12.1, reads the specified instance, sets the specified timelimit and the specified parameters, displayes all parameter settings, and solves the instance using optimize. All output is directed to $stdout."
	out.puts "USAGE: ruby run_cplex.rb <inst_file> <cutofftime> [parameters in format paramName=value, where paramName is the interactive command for setting parameters, without the \"set\" and with space replaced by _]"
end

if ARGV.length < 2
	output_help($stdout)
	exit
end

inst_filename = ARGV[0]
cutoff_time = ARGV[1]

#=== I'm calling ruby on the command line to start an interactive CPLEX session and write the parameter settings into it. The output is written to the specified output file.
cmd = "ruby -e 'File.popen(\"#{cplex_executable_absolute_filename}\",\"w\"){|file| "

# Use a temporary directory as workdir
dir = Dir.mktmpdir(["cplex", "tmp"], tmpdir)
begin
    cplex_lines = []
    #cplex_lines << "set logfile *" # disables the log file
    cplex_lines << "read #{inst_filename}"
    cplex_lines << "set clocktype 1"
    cplex_lines << "set threads 1"
    cplex_lines << "set workmem 40000"
    cplex_lines << "set mip strategy file 3"
    cplex_lines << "set timelimit #{cutoff_time}"
    cplex_lines << "set workdir #{dir}"
    
    ##== BAD LINE ==#
    #cplex_lines << "set workdir /ubc/cs/project/arrow/hutter/mip_configuration/cplex/cpx12te_tmp_files"

    if inst_filename =~ /_obj_max/
	    cplex_lines << "change sense obj max"
    end

    #=== Set parameters.
    for i in 2...ARGV.length
	    if ARGV[i] =~ /=.*=/ or !ARGV[i] =~ /=/
		    output_help($stdout)
		    puts "Each param=value pair must have exactly one \"=\" sign. The following argument doesn't: #{ARGV[i]}"
		    exit -1
	    end
	
	    param,value = ARGV[i].split("=")
	    param.gsub!(/_/," ")
	    cplex_lines << "set #{param} #{value}"
	    if param =~ /mip strategy file/
		    unless value.to_s == "0" or value.to_s == "1"
			    raise "problem with parsing: node file must be 0 or 1; was #{value}"
		    end
	    end
    end

    cplex_lines << "display settings all"
    cplex_lines << "opt"
    cplex_lines << "quit"

    cplex_lines.map{|line| cmd += "file.puts \"#{line}\"; "}
    cmd += "}'"

    puts "Calling: #{cmd}"
    system cmd
ensure
    # Remove the directory
    # (May want to use remove secure)
    FileUtils.remove_dir(dir)
end
