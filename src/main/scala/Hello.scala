package example

import java.io.{FileDescriptor, FileInputStream, FileOutputStream}
object Hello extends Greeting with App {

  println(greeting)

  val fis = new FileInputStream("/etc/hosts")

  // 2. We extract the underlying OS file descriptor
  val fd: FileDescriptor = fis.getFD

  // 3. The FD proves the stream is securely tied to a system resource
  println(s"Is the file descriptor valid and open? ${fd.valid()}")

  // 4. We must release the descriptor back to the OS when done
  fis.close()
  println(s"Is it valid after closing? ${fd.valid()}")

}

trait Greeting {
  lazy val greeting: String = "hello"
}
