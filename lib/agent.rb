require "ruby_llm"

key_path = File.join(__dir__, "key.txt")

RubyLLM.configure do |config|
  config.deepseek_api_key = File.read(key_path)
end

class ExecuteRuby < RubyLLM::Tool
  attr_accessor :repl_binding

  def execute(code:)
    dangerous = disallowed_token(code)
    return "Blocked: #{dangerous}. Please stick to things like loops, threads, basic data types, etc." if dangerous

    eval(code, repl_binding).inspect
  rescue StandardError => e
    "Error: #{e.class}: #{e.message}"
  end
end

def run_agent(task, repl_binding)
  system_prompt_path = File.join(__dir__, "system_prompt.txt") # this system prompt sucks im sorry lol
  system_prompt = File.read(system_prompt_path)

  tool = ExecuteRuby.new
  tool.repl_binding = repl_binding

  chat = RubyLLM.chat(model: "deepseek-v4-flash").with_instructions(system_prompt).with_thinking(effort: :low).with_tool(tool)
  response = chat.ask(task)
end
