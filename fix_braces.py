import re

with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "r") as f:
    content = f.read()

# Remove the extra `}` before KeyboardSubPanel.Apps
content = content.replace(
"""                    }
                }
                }
                KeyboardSubPanel.Apps -> {""",
"""                    }
                }
                KeyboardSubPanel.Apps -> {""")

# Remove the extra `}` before KeyboardSubPanel.Gif
content = content.replace(
"""                    }
                }
                }
                KeyboardSubPanel.Gif -> {""",
"""                    }
                }
                KeyboardSubPanel.Gif -> {""")

# At the end of Gif there is an extra `}` probably, let's look at the end of `when`
# We have:
#                KeyboardSubPanel.Gif -> {
#                    ...
#                }
#                }
#            }
#        }
#    }
#}
# Let's clean up the end of KeyboardSubPanel.Gif
content = content.replace(
"""                    }
                }
                }
            }
        }
    }
}
}""",
"""                    }
                }
            }
        }
    }
}""")

content = content.replace(
"""                    }
                }
                }
            }
        }
    }
}""",
"""                    }
                }
            }
        }
    }
}""")

with open("app/src/main/java/com/example/ui/keyboard/KeyboardLayout.kt", "w") as f:
    f.write(content)
