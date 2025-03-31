require "zlib"
require 'fileutils'

def rgss_main(&block)
    Java::OrgJrgss::JRGSSGame::jrgssMain(block)
end

class << Dir

    alias_method(:jrgss_glob, :glob)
    def glob(*args, &block)
        path = args[0]
        args.delete_at(0)
        if block
            result = []
            for p in $_jrgss_paths
                        Dir.chdir p do
                            result << jrgss_glob(path, *args)
                        end
            end
            for r in result.flatten
                block.call(r)
            end
         else
            result = []
            for p in $_jrgss_paths
                                    Dir.chdir p do
                                        result << jrgss_glob(path, *args)
                                    end
                        end
            return result.flatten
         end
    end

end


class << File

    alias_method(:jrgss_open, :open)
    def open(path, *args, &block)
        if path.start_with?(Java::JavaIo::File.separator) || path.match('\A[A-Za-z]:')
            return jrgss_open(path, *args, &block)
        end
        exception = nil
        FileUtils.mkdir_p(File.join($_jrgss_paths[0],File.dirname(path)))
        for p in $_jrgss_paths
            begin
                if block
                    return jrgss_open(File.join(p,path), *args, &block)
                else
                    return jrgss_open(File.join(p,path), *args)
                end
            rescue Exception => e
                exception = e
            end
        end
        raise exception
    end

    alias_method(:jrgss_delete, :delete)
        def delete(path, *args)
            if path.start_with?(Java::JavaIo::File.separator) || path.match('\A[A-Za-z]:')
                return jrgss_delete(path, *args)
            end
            exception = nil
            for p in $_jrgss_paths
                begin
                    return jrgss_delete(File.join(p,path), *args)
                rescue Exception => e
                    exception = e
                end
            end
            raise exception
        end

end



def load_data(filename)
    filename = filename.gsub("\\", Java::JavaIo::File::separator).gsub("/", Java::JavaIo::File::separator)
    f = Java::OrgJrgss::FileUtil::rawLoadFile(filename)
    begin
        obj = Marshal.load(f)
    rescue Exception => e
	   obj = f
    end
    obj
end

def save_data(obj, filename)
    filename = filename.gsub("\\", Java::JavaIo::File::separator)
    filename = File.join($_jrgss_paths[0],filename)
    FileUtils.mkdir_p(File.dirname(filename))
    File.open(filename, "wb") { |f|
      Marshal.dump(obj, f)
    }
end


# Some scripts alias the load method. Unfortunately, JRuby doesn't handle nil procs/blocks very well.
# This is a workaround
class << Marshal
  alias_method(:jrgss_load, :load)
  def load(port, proc = nil)
    if proc
        jrgss_load(port, proc)
    else
        begin
            obj = jrgss_load(port)
        rescue Exception => e
            raise e
        end
        obj
    end
  end
end unless Marshal.respond_to?(:jrgss_load)