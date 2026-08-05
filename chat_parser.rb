# Should also include system messages, achievement messages, etc.

def player_messages(up_to)
  path = File.join(__dir__, "server", "logs", "latest.log")
  latest_system_logs = File.read(path).lines[-up_to..-1].reverse # Grab last message -> *up to* specified amount

  return latest_system_logs.map do |element|
    element.scan(/<\w+> ([\w ]+)/)
  end
end

def last_player_name
  path = File.join(__dir__, "server", "logs", "latest.log")
  latest_system_logs = File.read(path).lines[-1] # not a great way to do this
  latest_system_logs.scan(/<([^>]*)>/).join
end
