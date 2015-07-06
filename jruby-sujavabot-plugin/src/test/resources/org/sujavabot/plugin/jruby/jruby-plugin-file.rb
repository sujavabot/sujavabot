require 'java'
class TestFilePlugin
  include org.sujavabot.core.Plugin

  def getName()
    "file"
  end

  def initializePlugin()
  end

  def initializeBot(bot)
  end
  
end

TestFilePlugin.new
