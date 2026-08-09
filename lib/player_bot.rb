require "rcon"
require "json"
require "pathname"
require_relative "coords_parser"
require_relative "chat_parser"

class Bot
  attr_reader :stopped, :idle
  attr_writer :name, :skin_name, :message

  def initialize(name = "Steve", skin_name = "WoofAI")
    client = Rcon::Client.new(host: "0.0.0.0", port: 25575, password: "1234")
    @client = client
    @name = name
    @stopped = true
    @currently_sneaking = false
    client.authenticate!(ignore_first_packet: false)
    client.execute("fp spawn #{name}", wait: "0.25")
    client.execute("fp skin #{skin_name}", wait: "0.25")
    client.execute("op #{name}", wait: "0.25")
  end

  def stop
    @stopped = true
    return "Stopped."
  end

  def scan(parsed: true, radius: 2)
    @client.execute("fp scan #{radius}", wait: "0.25")
    path = Pathname.new(__dir__).join("..", "server", "plugins", "fakeplayer", "scans", "latest.json").expand_path
    file_content = File.read(path)
    return JSON.parse(file_content) if parsed
    return file_content if !parsed
  end

  def say(message)
    @client.execute("fp cmd #{@name} tellraw @a [\"\",{\"text\":\"<\",\"color\":\"white\"},{\"text\":\"#{@name}\",\"color\":\"#E05B5B\"},{\"text\":\"> \",\"color\":\"white\"},{\"text\":\"#{message}\",\"color\":\"white\"}]", wait: "0.25")
    return "Message sent."
  end

  def use
    @client.execute("fp use", wait: "0.25")
    return "Used item."
  end

  def jump
    @client.execute("fp jump", wait: "0.25")
    return "Jumped."
  end

  def mine
    @client.execute("fp mine", wait: "0.25")
    return "Mining."
  end

  def attack
    @client.execute("fp attack", wait: "0.25")
    return "Attacking once."
  end

  def walk
    @client.execute("fp move forward", wait: "0.25")
    return "Walking forward about 3 blocks."
  end

  def look(direction)
    @client.execute("fp look #{direction}", wait: "0.25")
    return "Looking #{direction}."
  end

  def command(string)
    @client.execute("fp cmd #{@name} #{string}", wait: "0.25")
    return "Command #{string} sent."
  end

  def drop
    @client.execute("fp drop", wait: "0.25")
    return "Dropped item or block from hand."
  end

  def drop_stack
    @client.execute("fp dropstack", wait: "0.25")
    return "Dropped entire stack of item or block from hand."
  end

  def hold(slot)
    @client.execute("fp hold #{slot}", wait: "0.25")
    return "Now holding slot number #{slot}."
  end

  def sneak
    @client.execute("fp sneak", wait: "0.25")

    if !@currently_sneaking
      @currently_sneaking = true
      return "Now currently sneaking. Sneak toggle ON."
    else
      @currently_sneaking = false
      return "No longer sneaking. Sneak toggle OFF."
    end
  end

  def look_at(thing)
    file_content = scan(parsed: false, radius: 10)
    result_coords = JSON.parse(/\{"(?:block|type)":"minecraft:[a-z_]*#{Regexp.escape(thing)}[a-z_]*"[^}]*\}/i.match(file_content)[0])["pos"].join(" ") rescue nil
    @client.execute("fp look at #{result_coords}", wait: "0.25") if result_coords
    puts "Looking at first '#{thing}'. If found, the coordinates will show here -> #{result_coords}."
    return result_coords
  end

  def look_at_coords(coords)
    return "Incorrect format. Minecraft only takes 'num num num'. No brackets or commas." if !coords.match(/^-?\d+ -?\d+ -?\d+$/)
    @client.execute("fp look at #{coords}", wait: "0.25")
    return "Looking at '#{coords}'."
  end

  def go_to(thing)
    @stopped = false
    sprint_toggle = false
    swim_toggle = false
    target_coords = look_at(thing).split.map(&:to_f)

    loop do
      return "Arrived to target '#{thing}' at coordinates #{target_coords}." if @stopped

      parsed_bot_coords = coords_parser(@client.execute("data get entity #{@name} Pos", wait: "0.25").body)
      distance_from_target = (parsed_bot_coords[:proper_array].sum - target_coords.sum).abs
      target_higher_than_bot = parsed_bot_coords[:proper_array][1] < target_coords[1]

      should_swim = true if scan["block_at_feet"]["block"] == "minecraft:water" && scan["block_below"]["block"] == "minecraft:water"
      should_swim = false if scan["block_at_feet"]["block"] != "minecraft:water" && scan["block_below"]["block"] != "minecraft:water"

      if should_swim && !swim_toggle
        @client.execute("fp jump", wait: "0.25")
        @client.execute("fp jump", wait: "0.25")
        swim_toggle = true
      elsif !should_swim
        swim_toggle = false
      end

      if distance_from_target >= 10 && !sprint_toggle
        @client.execute("fp sprint", wait: "0.25")
        sprint_toggle = true
      elsif distance_from_target <= 9 && (sprint_toggle || @stopped)
        @client.execute("fp sprint", wait: "0.25")
        sprint_toggle = false
      end

      @client.execute("fp look at #{target_coords}", wait: "0.25")
      @client.execute("fp move forward", wait: "0.25")
      @client.execute("fp jump", wait: "0.25") if target_higher_than_bot
      @stopped = true if distance_from_target < 2
      sleep 0.75
    end
    @stopped = true
  end
end

