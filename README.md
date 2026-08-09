# Ruby on Redstone ⛏️

**A highly experimental *rough* proof of concept Ruby-based Minecraft bot controlled through RCON** 

My first swing at something like this. I discovered [this fake player plugin](https://github.com/tanyaofei/minecraft-fakeplayer) and thought it would be cool if I could use rcon-rb to interface with it through Ruby. And yes, you can!

Here, we make a semi-sandboxed REPL environment, give an LLM a bit of context, and then instruct the LLM to do whatever you want. Since the bot player has OP, full in-game chat access, and a nearly full Ruby environment, there's a lot of possibilities. **HOWEVER!** That being said...

> ⚠️ This is not a truly sandboxed REPL environment. If the LLM wanted, it could easily escape. It's about as dangerous as running an LLM in YOLO mode on your harness overnight. Which some may argue is not that dangerous at all. Use at your own caution. Again, this is merely a small hobbiest project/proof of concept.

Here's some examples with DeepSeek v4 Flash on low thinking effort (sped up and cut):

| Video | Description |
|-------|-------------|
| <video src="https://github.com/user-attachments/assets/e2b33327-e195-4389-ac0e-09e2d4e2f7d4" width="320" height="240" controls></video> | **Example 1** — Putting a nametag on a pig and then accepting further instructions to attack it |
| <video src="https://github.com/user-attachments/assets/be3b9b4e-3c18-4d77-9c1c-69dca58b5550" width="320" height="240" controls></video> | **Example 2** — Teleporting to me and then doing a dance. 2 actions that are not pre-coded. |
| <video src="https://github.com/user-attachments/assets/3488ade1-762f-42d8-8b8d-6b6861cc8714" width="320" height="240" controls></video> | **Example 3** — Spawning itself an axe, finding the nearest tree, chopping it down, and attempting to give me the wood. DeepSeek seems to get confused and decides to /give wood to my name instead of dropping it's own. |
| <video src="https://github.com/user-attachments/assets/66b72706-0102-4dae-b234-f475f764ab02" width="320" height="240" controls></video> | **Example 4** — Teleporting to me and barely suceeding in destroying a sign. |
| <video src="https://github.com/user-attachments/assets/b46d8bef-f417-4917-ba6c-38cc80647cc7" width="320" height="240" controls></video> | **Debug Mode (REPL)** — Manually write Ruby scripts with the same tooling as the bot has. Here's a bad example that I can't be bothered to record again. |

> The FakePlayer plugin has been slightly modified to support 26.2 as well as add some niceties like grabbing every block and entity coordinate within a radius. I don't know Java, so... thank you China :)

## Current Limitations

- [ ] RCON client automatically drops after 12000 ticks. Kicking the bot.
- [ ] **SEVERE** movement and "eyesight" limitations. Essentially playing with a quarter chunk render distance. (Vision model? 🤔)
- [ ] The REPL sandbox is very weak, purely a "better than nothing".
- [ ] LLM response can be slow, even on a flash model with low thinking. Perhaps a better system prompt would help.
- [ ] No memory, each turn is fresh, although the LLM may pickup on context clues. Simple fix, but context may get bloated.

> All in all, pretty neat! I think there's some potential. DeepSeek on higher effort thinking can get very intricate. I once asked it to spawn it's own fake players using the plugin and indepdendently control each one, randomly moving around and talking in chat. It successfully did so with 10 fake players concurrently.

## Credits & Acknowledgments

This project is built on top of the following open-source work:

- [**rcon-rb**](https://codeberg.org/hernanat/rconrb) — Ruby RCON client used to communicate with the Minecraft server.
- [**ruby_llm**](https://github.com/crmne/ruby_llm) — Ruby framework used for LLM integration. Licensed under MIT.
- [**minecraft-fakeplayer**](https://github.com/tanyaofei/minecraft-fakeplayer) — Server-side plugin used to spawn and control the bot player.

---

## License

This project is licensed under the [MIT License](LICENSE) - a.k.a. idrc what you do lol
