import google.generativeai as genai
import traceback

try:
    genai.configure(api_key='AIzaSyCGq7WjlqgCReBcMmLqa1vQkb7fH7DHzZA')
    for m in genai.list_models():
        if 'generateContent' in m.supported_generation_methods:
            print(m.name)
except Exception as e:
    print("ERROR:")
    traceback.print_exc()
