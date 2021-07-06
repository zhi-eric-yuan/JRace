# JRace wrapper for CPLEX 12.6
# Adopted from http://www.cs.ubc.ca/labs/beta/Projects/MIP-Config/cplex_wrapper.rb, modified by Zhi Eric Yuan 

$path_to_runcplex_script = "cplex_vfp";
#$path_to_runcplex_script = ".";

def float_regexp()
        return '[+-]?\d+(?:\.\d+)?(?:[eE][+-]\d+)?';
end

def parse_cplex_output(output_file, quality_for_verification, allowed_mipgap, timeout)
	solved = "CRASHED"
	seed = -1
	best_length = -1
	measured_runlength = -1
	measured_runtime = -1
    
	gap = 1e100
	obj = 1e100
	
	slack_in_my_assertions = 1.0 # multiplicative, i.e. no slack.
    
	File.open(output_file, "r"){|file|
		while line = file.gets
#			puts "Read line: #{line}"
			
			#########################################################################
			#===  Parsing CPLEX run output
			#########################################################################
			if line =~ /\(gap = #{float_regexp}, (#{float_regexp})%\)/
				gap = $1.to_f
			end
				
			if line =~ /MIP\s*-\s*Integer optimal solution:\s*Objective\s*=\s*(#{float_regexp})/
				gap = 0
				obj = $1.to_f
				solved = 'SAT'
			end

			if line =~ /MIP\s*-\s*Integer optimal,\s*tolerance\s*\(#{float_regexp}\/#{float_regexp}\):\s*Objective\s*=\s*(#{float_regexp})/
				obj = $1.to_f
				solved = 'SAT'
			end
						
			if line =~ /Solution time\s*=\s*(#{float_regexp})\s*sec\.\s*Iterations\s*=\s*(\d+)\s*Nodes\s*=\s*(\d+)/
				measured_runtime = $1
				iterations = $2
				measured_runlength = $3
			end

			if line =~ /Solution time =\s*(#{float_regexp}) sec\./
				measured_runtime = $1
				raise "CPLEX reports negative time. I thought this bug was fixed in version 12.6." if measured_runtime.to_f < 0
			end
			
			if line =~ /Optimal:\s*Objective =\s*#{float_regexp}/
				solved = 'SAT'
			end

			if line =~ /Infeasible/
				#raise "CPLEX claims instance to be infeasible"
				solved = 'WRONG ANSWER'
			end
			
			if line =~/MIP - Integer infeasible./
				solved = 'WRONG ANSWER'
			end
				
					
#		if line =~ /Current MIP best bound is infinite./ 
#			solved = "TIMEOUT"
#			if line = file.gets
#				if line =~/Solution time =  #{float_regexp} sec\./
#					runtime = timeout + 0.01 # override that output, it's not accurate.
#				end
#			end
#		end
			
			if line =~ /MIP\s*-\s*Time limit exceeded, integer feasible:\s*Objective\s*=\s*(#{float_regexp})/
				obj = $1.to_f
				solved = 'TIMEOUT'
			end

			if line =~ /MIP\s*-\s*Error termination, integer feasible:\s*Objective\s*=\s*(#{float_regexp})/
				obj = $1.to_f
				solved = 'TIMEOUT'
			end

			if line =~ /MIP - Time limit exceeded, no integer solution./
				solved = 'TIMEOUT'
			end
			
			if line =~ /CPLEX Error  1001: Out of memory./
				solved = 'TIMEOUT'
			end
			
			if line =~ /CPLEX Error  3019: Failure to solve MIP subproblem./
				solved = 'TIMEOUT'
			end
			
			if line =~ /Restricted version.  Problem size limits exceeded./ # sometimes this only happens with particular parameter settings!
				raise "CPLEX output: " + line
				#solved = 'TIMEOUT'
			end

#			if line =~ /CPLEX Error/
#				solved = 'TIMEOUT'
#			end
			
			if line =~ /Time limit exceeded/
				solved = 'TIMEOUT'
			end
			
			if line =~ /Filesize limit exceeded/
				solved = 'TIMEOUT'
			end
			
			if line =~ /No problem exists./
				solved = 'CRASHED'
			end
			
			if line =~ /Polishing requested, but no solution to polish./
				solved = 'TIMEOUT'
			end
			
			raise line if line =~ /Failed to initialize CPLEX environment./
			raise line if line =~ /ILM Error 15: CPLEX: no license available/
		end

=begin
		if solved == "CRASHED"
			puts "\n\n==============================================\n\nWARNING: CPLEX crashed -> most likely file not found or no license\n\n=======================\n"
			raise "No such file or directory: CPLEX crashed -> most likely file not found or no license\n\n======================="
		end
=end
	}
	
	if solved == "SAT"
		raise "solved by no objective value report --- must be a parsing problem" unless obj

		#=== Check correctness.
		unless quality_for_verification.to_f == 0 || quality_for_verification == "instance_specific" # for backwards compatibility with my previous runs
			maxi = [obj.abs, quality_for_verification.to_f.abs].max
			if (obj.abs - quality_for_verification.to_f.abs).abs/maxi > slack_in_my_assertions * allowed_mipgap  && (obj.abs - quality_for_verification.to_f.abs).abs > 1e-8
				solved = "WRONG ANSWER" 
				#raise "CPLEX claims to have solved the instance, but its result (#{obj.abs}) differs from the actual one (#{quality_for_verification.to_f.abs}) by more than a relative error of 0.01%."
			end
		end
	end
	
	if (solved == "SAT" or solved =="UNSAT") and measured_runtime.to_f > timeout.to_f
		solved = "TIMEOUT"
	end
	
	puts "Result for ParamILS: #{solved}, #{measured_runtime}, #{obj}, #{gap}"  # mis-using the runlength field to store objective reached
	puts "#{measured_runtime} #{gap} #{solved}"
	return [solved, measured_runtime, obj, gap]
end

def get_instance_specifics(input_file)
        return 0 # disables verification of solution quality.
	input_file =~ /MIP_data(.*)/
        name = $1
        name = "MIP_data" + name
        File.open("/ubc/cs/project/arrow/hutter/instance_list_obj.txt"){|file|
                while line = file.gets
                        filename, opt = line.split
                        if filename =~ /MIP_data(.*)/
                                filename = $1
                                filename = "MIP_data" + filename
                                if filename == name
                                        return opt
                                end
                        end
                end
        }
        raise "need to include objective for input file #{input_file} in file /ubc/cs/project/arrow/hutter/instance_list_obj.txt"
end

def wrap_cplex(argv, cplex_rubyfile)
	tmpdir = "/tmp"

	input_file = argv[0]
	
	#=== Here instance_specifics are used to verify the result CPLEX computes.
	#instance_specifics = argv[1]
	
	#===Ignore that input; rather check instance specifics myself.
	instance_specifics = get_instance_specifics(input_file)
	
	timeout = argv[1].to_f
	#cutoff_length = argv[2].to_i
	#seed = argv[2].to_i

	allowed_mipgap = 0.0001
	slack_in_my_assertions = 1.0 # i.e., none

	#=== I'm calling ruby on the command line to start an interactive CPLEX session and write the parameter settings into it. The output is written to the specified output file.
	cmd = "ruby #{cplex_rubyfile} #{input_file} #{timeout}"

	#p argv[5]
	#p argv[6]

	i = 2
	if argv[2] == "-param_string" and not argv[3] == "default-params"
		i=3 # this helps to specify local modifications of the default as returned by the CPLEX tuning tool.
	end
	unless argv[2] == "-param_string" and argv[3] == "default-params" 
		simplex_perturbation_switch = "no"
		perturbation_constant = "1e-6"
		while i <= argv.length-2
			param = argv[i].sub(/^-/,"")
			
			case param
				#=== Deal with 2 parameters in one in "simplex perturbation"
				when "simplex_perturbation_switch"
					simplex_perturbation_switch = argv[i+1]
				when "perturbation_constant"
					perturbation_constant = argv[i+1]
				
				#=== Deal with relative time solution for solution polishing
				when "mip_polishafter_time_rel"
					relative_time_percent = argv[i+1].to_f
					absolute_time = (timeout+0.1) * relative_time_percent / 100
					cmd << " mip_polishafter_time=#{absolute_time}"
				
				else			
					cmd << " #{param}=#{argv[i+1]}"
			end
			i += 2
		end
		#===  Very annoying, two parameters in one! No auto. Binary & R+ (>=1e-8) for the two params. This is also not discussed in the parameters reference manual.
		cmd << " 'simplex_perturbation=#{simplex_perturbation_switch} #{perturbation_constant}'"
	end

	outfile = "#{tmpdir}/cplex-out-#{rand}.txt"

	numtry = 1
	begin
		puts "Calling: #{cmd} > #{outfile}"
		system("#{cmd} > #{outfile}")

		inner_exit = $?
		puts "inner exit: #{inner_exit}"

		result = parse_cplex_output(outfile, instance_specifics, allowed_mipgap, timeout)
		raise "run crashed: #{cmd}" if result[0] == "CRASHED"
		File.delete(outfile)
	rescue 
		puts $!
		sleep(10)
		numtry = numtry + 1
		retry if numtry <= 5 # to safeguard against temporary problems with the file system etc
	end
end


# Deal with inputs.
if ARGV.length < 2
    puts "cplex_wrapper.rb is a wrapper for CPLEX (currently, version 12.6)"
    puts "Usage: ruby cplex_wrapper.rb <instance_relname> <cutoff_time> <params to be passed on>."
    exit -1
end

wrap_cplex(ARGV, "#{$path_to_runcplex_script}/run_cplex.rb")

