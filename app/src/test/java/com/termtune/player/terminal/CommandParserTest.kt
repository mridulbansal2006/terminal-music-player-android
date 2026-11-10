package com.termtune.player.terminal

import com.termtune.player.ui.theme.TerminalTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandParserTest {

    private fun parsed(input: String): Command {
        val result = CommandParser.parse(input)
        assertTrue("expected $input to parse, got $result", result is ParseResult.Parsed)
        return (result as ParseResult.Parsed).command
    }

    private fun invalid(input: String): ParseResult.Invalid {
        val result = CommandParser.parse(input)
        assertTrue("expected $input to be invalid, got $result", result is ParseResult.Invalid)
        return result as ParseResult.Invalid
    }

    @Test
    fun `blank input is empty`() {
        assertEquals(ParseResult.Empty, CommandParser.parse(""))
        assertEquals(ParseResult.Empty, CommandParser.parse("    "))
    }

    @Test
    fun `every documented command parses`() {
        assertEquals(Command.Help, parsed("help"))
        assertEquals(Command.ListSongs, parsed("list"))
        assertEquals(Command.Play(null), parsed("play"))
        assertEquals(Command.Pause, parsed("pause"))
        assertEquals(Command.Resume, parsed("resume"))
        assertEquals(Command.Next, parsed("next"))
        assertEquals(Command.Prev, parsed("prev"))
        assertEquals(Command.Stop, parsed("stop"))
        assertEquals(Command.NowPlaying, parsed("nowplaying"))
        assertEquals(Command.Volume(50), parsed("volume 50"))
        assertEquals(Command.Seek(30), parsed("seek 30"))
        assertEquals(Command.Shuffle(true), parsed("shuffle on"))
        assertEquals(Command.Shuffle(false), parsed("shuffle off"))
        assertEquals(Command.Repeat(true), parsed("repeat on"))
        assertEquals(Command.Repeat(false), parsed("repeat off"))
        assertEquals(Command.Search("bohemian"), parsed("search bohemian"))
        assertEquals(Command.Clear, parsed("clear"))
        assertEquals(Command.SetTheme(TerminalTheme.GREEN), parsed("theme green"))
        assertEquals(Command.SetTheme(TerminalTheme.AMBER), parsed("theme amber"))
        assertEquals(Command.SetTheme(TerminalTheme.BLUE), parsed("theme blue"))
    }

    @Test
    fun `commands are case insensitive and whitespace tolerant`() {
        assertEquals(Command.Help, parsed("HELP"))
        assertEquals(Command.ListSongs, parsed("  List  "))
        assertEquals(Command.Volume(80), parsed("Volume    80"))
    }

    @Test
    fun `aliases map to the documented commands`() {
        assertEquals(Command.ListSongs, parsed("ls"))
        assertEquals(Command.NowPlaying, parsed("np"))
        assertEquals(Command.Volume(10), parsed("vol 10"))
        assertEquals(Command.Clear, parsed("cls"))
        assertEquals(Command.Next, parsed("skip"))
        assertEquals(Command.Prev, parsed("back"))
        assertEquals(Command.Search("queen"), parsed("find queen"))
    }

    @Test
    fun `play accepts numbers and multi word names`() {
        assertEquals(Command.Play("3"), parsed("play 3"))
        assertEquals(Command.Play("bohemian rhapsody"), parsed("play bohemian rhapsody"))
    }

    @Test
    fun `search keeps the whole query`() {
        assertEquals(Command.Search("dark side of the moon"), parsed("search dark side of the moon"))
    }

    @Test
    fun `unknown commands are reported by name`() {
        val result = CommandParser.parse("sudo rm -rf /")
        assertTrue(result is ParseResult.Unknown)
        assertEquals("sudo", (result as ParseResult.Unknown).name)
    }

    @Test
    fun `volume rejects bad input`() {
        assertTrue(invalid("volume").message.contains("missing"))
        assertTrue(invalid("volume abc").message.contains("not a number"))
        assertTrue(invalid("volume 150").message.contains("out of range"))
        assertTrue(invalid("volume -5").message.contains("out of range"))
        assertTrue(invalid("volume 10 20").message.contains("too many"))
    }

    @Test
    fun `volume accepts a percent suffix`() {
        assertEquals(Command.Volume(40), parsed("volume 40%"))
    }

    @Test
    fun `seek rejects bad input`() {
        assertTrue(invalid("seek").message.contains("missing"))
        assertTrue(invalid("seek abc").message.contains("not a valid position"))
        assertTrue(invalid("seek -3").message.contains("negative"))
    }

    @Test
    fun `seek accepts timecodes`() {
        assertEquals(Command.Seek(90), parsed("seek 1:30"))
        assertEquals(Command.Seek(3690), parsed("seek 1:01:30"))
        assertEquals(90, CommandParser.parseTimecode("1:30"))
        assertEquals(null, CommandParser.parseTimecode("1:99"))
        assertEquals(null, CommandParser.parseTimecode("a:b"))
    }

    @Test
    fun `toggles reject unknown states`() {
        val shuffle = invalid("shuffle maybe")
        assertTrue(shuffle.message.contains("unknown state"))
        assertEquals("usage: shuffle on|off", shuffle.usage)
        assertTrue(invalid("repeat").message.contains("missing state"))
    }

    @Test
    fun `theme rejects unknown names`() {
        val result = invalid("theme pink")
        assertTrue(result.message.contains("unknown theme"))
        assertEquals("usage: theme green|amber|blue", result.usage)
        assertTrue(invalid("theme").message.contains("missing name"))
    }

    @Test
    fun `search requires a query`() {
        assertTrue(invalid("search").message.contains("missing query"))
    }

    @Test
    fun `commands without arguments reject extra words`() {
        assertTrue(invalid("pause now").message.contains("takes no arguments"))
        assertTrue(invalid("stop everything").message.contains("takes no arguments"))
    }

    @Test
    fun `every help entry is a parseable example`() {
        HelpText.commands.forEach { doc ->
            val result = CommandParser.parse(doc.example)
            assertTrue(
                "help example '${doc.example}' did not parse: $result",
                result is ParseResult.Parsed,
            )
        }
    }
}
