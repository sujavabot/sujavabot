require 'java'
class TestFilePlugin
  include org.sujavabot.core.Plugin

  def getName()
    "file"
  end

  def load(bot)
  end

  def unload(bot)
  end
  
end

TestFilePlugin.new
