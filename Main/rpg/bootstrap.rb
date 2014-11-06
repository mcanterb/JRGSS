require "zlib"

def rgss_main(&block)
    Java::OrgJrgss::JRGSSGame::jrgssMain(block)
end



def load_data(filename)
    filename = filename.gsub("\\", "/")
    puts 'Loading Data for '+filename
    f = Java::OrgJrgss::FileUtil::rawLoadFile(filename)
    obj = Marshal.load(f)

    obj
end

def save_data(obj, filename)
    filename = filename.gsub("\\", "/")
    puts 'Saving Data for '+filename
    File.open(File.join($_jrgss_home,filename), "wb") { |f|
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
        jrgss_load(port)
    end
  end
end unless Marshal.respond_to?(:jrgss_load)