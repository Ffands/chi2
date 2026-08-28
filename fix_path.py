with open('app/src/main/java/com/example/autoclicker/AutoClickService.kt', 'r') as f:
    text = f.read()

target = """                        if (node.randomizeRadius > 0) {
                            val angle = Math.random() * Math.PI * 2
                            val r = Math.random() * node.randomizeRadius
                            eX += (Math.cos(angle) * r).toFloat()
                            eY += (Math.sin(angle) * r).toFloat()
                        }
                        path.lineTo(eX, eY)
                    }
                }
                val duration ="""

rep = """                        if (node.randomizeRadius > 0) {
                            val angle = Math.random() * Math.PI * 2
                            val r = Math.random() * node.randomizeRadius
                            eX += (Math.cos(angle) * r).toFloat()
                            eY += (Math.sin(angle) * r).toFloat()
                        }
                        path.lineTo(eX, eY)
                    }
                } else {
                    path.lineTo(startX + 1f, startY + 1f)
                }
                val duration ="""

text = text.replace(target, rep)

with open('app/src/main/java/com/example/autoclicker/AutoClickService.kt', 'w') as f:
    f.write(text)
