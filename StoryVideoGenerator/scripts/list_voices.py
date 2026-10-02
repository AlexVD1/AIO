import os
import edge_tts
import asyncio

async def list_es_voices():
    voices = await edge_tts.list_voices()
    for v in voices:
        if v["Locale"].startswith("es-"):
            print(f"{v['ShortName']} ({v['Gender']}) - {v['Locale']} - {v['FriendlyName']}")

asyncio.run(list_es_voices())
