/** Loopback-only SMTP capture for isolated browser tests. Never forwards or logs message contents. */
import net from 'node:net'

export function createAccountTestSmtp(port = 12525) {
  const messages = [], sockets = new Set(), metrics = { connections: 0, commands: 0, messages: 0 }
  const server = net.createServer(socket => {
    metrics.connections++
    sockets.add(socket); socket.on('close', () => sockets.delete(socket))
    socket.setEncoding('utf8'); socket.write('220 localhost account test SMTP\r\n')
    let buffer = '', data = false, lines = [], recipients = []
    socket.on('data', chunk => {
      buffer += chunk
      let newline
      while ((newline = buffer.indexOf('\n')) >= 0) {
        const line = buffer.slice(0, newline).replace(/\r$/, ''); buffer = buffer.slice(newline + 1)
        if (data) {
          if (line !== '.') { lines.push(line.replace(/^\.\./, '.')); continue }
          messages.push({ recipients: [...recipients], raw: lines.join('\r\n') })
          metrics.messages++
          data = false; lines = []; socket.write('250 accepted\r\n'); continue
        }
        const command = line.split(' ')[0].toUpperCase()
        metrics.commands++
        if (command === 'EHLO' || command === 'HELO') socket.write('250-localhost\r\n250 8BITMIME\r\n')
        else if (command === 'MAIL') { recipients = []; socket.write('250 sender accepted\r\n') }
        else if (command === 'RCPT') { recipients.push(line.match(/<([^>]+)>/)?.[1] || ''); socket.write('250 recipient accepted\r\n') }
        else if (command === 'DATA') { data = true; lines = []; socket.write('354 end with dot\r\n') }
        else if (command === 'QUIT') socket.end('221 bye\r\n')
        else if (command === 'RSET' || command === 'NOOP') socket.write('250 ok\r\n')
        else socket.write('502 unsupported\r\n')
      }
    })
  })
  return {
    messages,
    metrics,
    start: () => new Promise((resolve, reject) => { server.once('error', reject); server.listen(port, '127.0.0.1', resolve) }),
    close: () => new Promise(resolve => { for (const socket of sockets) socket.destroy(); server.close(resolve) }),
    latest(email, pattern) {
      for (const message of [...messages].reverse()) {
        if (!message.recipients.includes(email)) continue
        const separator = message.raw.indexOf('\r\n\r\n'), header = message.raw.slice(0, separator)
        let body = message.raw.slice(separator + 4)
        if (/Content-Transfer-Encoding: base64/i.test(header)) body = Buffer.from(body.replace(/\s/g, ''), 'base64').toString('utf8')
        else if (/Content-Transfer-Encoding: quoted-printable/i.test(header)) {
          body = body.replace(/=\r\n/g, '').replace(/=([0-9a-f]{2})/gi, (_, byte) => String.fromCharCode(parseInt(byte, 16)))
        }
        const match = body.match(pattern)
        if (match) return match[1]
      }
      return ''
    },
  }
}
