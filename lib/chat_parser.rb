# Should also include system messages, achievement messages, etc.
require "pathname"

class ChatLog
  LOG = Pathname.new(__dir__).join("..", "server", "logs", "latest.log")

  def initialize
    @seen = lines.size
  end

  def new_messages
    current = lines
    @seen = 0 if current.size < @seen
    fresh = current.drop(@seen)
    @seen = current.size

    fresh.filter_map do |line|
      line[/<\w+> (.*)/, 1]
    end
  end
  private

  def lines
    File.readlines(LOG)
  end
end
